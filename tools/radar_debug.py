#!/usr/bin/env python3
"""Generate local radar/map alignment debug images for GabeCast.

The script fetches the same NOAA radar ImageServer layer and Esri basemap used by
the Android app, composites them locally, and draws a center marker plus the
requested radius. It is intentionally standalone so radar debugging does not
require rebuilding or installing the Android app.
"""

from __future__ import annotations

import argparse
import json
import math
import time
import urllib.parse
import urllib.request
from dataclasses import dataclass
from datetime import datetime, timezone
from io import BytesIO
from pathlib import Path
from typing import Iterable

from PIL import Image, ImageDraw, ImageFont


RADAR_SERVICE = "https://mapservices.weather.noaa.gov/eventdriven/rest/services/radar/radar_base_reflectivity_time/ImageServer"
RADAR_EXPORT = f"{RADAR_SERVICE}/exportImage"
BASEMAP_EXPORT = "https://services.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/export"
WEB_MERCATOR_WKID = 3857
EARTH_RADIUS_METERS = 6_378_137.0
METERS_PER_MILE = 1609.344
YPSILANTI_LAT = 42.2411
YPSILANTI_LON = -83.6130


@dataclass(frozen=True)
class Extent:
    min_x: float
    min_y: float
    max_x: float
    max_y: float
    wkid: int

    @property
    def width(self) -> float:
        return self.max_x - self.min_x

    @property
    def height(self) -> float:
        return self.max_y - self.min_y

    def bbox_param(self) -> str:
        return f"{self.min_x},{self.min_y},{self.max_x},{self.max_y}"


@dataclass(frozen=True)
class Variant:
    name: str
    description: str
    extent: Extent
    explicit_time: bool
    radius_miles_for_circle: float


def http_get(url: str, *, binary: bool = True) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": "GabeCastRadarDebug/1.0"})
    with urllib.request.urlopen(request, timeout=45) as response:
        payload = response.read()
        content_type = response.headers.get("Content-Type", "")
        if binary and "image" not in content_type:
            raise RuntimeError(f"Expected image from {url}, got {content_type}: {payload[:300]!r}")
        return payload


def build_url(base: str, params: dict[str, str | int | float]) -> str:
    return f"{base}?{urllib.parse.urlencode(params)}"


def fetch_json(url: str) -> dict:
    return json.loads(http_get(url, binary=False).decode("utf-8"))


def web_mercator_point(lat: float, lon: float) -> tuple[float, float]:
    clamped_lat = max(min(lat, 85.05112878), -85.05112878)
    x = EARTH_RADIUS_METERS * math.radians(lon)
    lat_rad = math.radians(clamped_lat)
    y = EARTH_RADIUS_METERS * 0.5 * math.log((1 + math.sin(lat_rad)) / (1 - math.sin(lat_rad)))
    return x, y


def web_mercator_extent(lat: float, lon: float, radius_miles: float) -> Extent:
    x, y = web_mercator_point(lat, lon)
    local_scale = max(math.cos(math.radians(lat)), 0.15)
    projected_radius_m = radius_miles * METERS_PER_MILE / local_scale
    return Extent(
        min_x=x - projected_radius_m,
        min_y=y - projected_radius_m,
        max_x=x + projected_radius_m,
        max_y=y + projected_radius_m,
        wkid=WEB_MERCATOR_WKID,
    )


def latlon_extent(lat: float, lon: float, radius_miles: float) -> Extent:
    lat_delta = radius_miles / 69.0
    lon_delta = radius_miles / (69.0 * max(math.cos(math.radians(lat)), 0.15))
    return Extent(
        min_x=lon - lon_delta,
        min_y=lat - lat_delta,
        max_x=lon + lon_delta,
        max_y=lat + lat_delta,
        wkid=4326,
    )


def map_to_pixel(extent: Extent, x: float, y: float, size: int) -> tuple[float, float]:
    px = (x - extent.min_x) / extent.width * size
    py = (extent.max_y - y) / extent.height * size
    return px, py


def latlon_to_pixel_web_mercator(extent: Extent, lat: float, lon: float, size: int) -> tuple[float, float]:
    x, y = web_mercator_point(lat, lon)
    return map_to_pixel(extent, x, y, size)


def latlon_to_pixel_latlon(extent: Extent, lat: float, lon: float, size: int) -> tuple[float, float]:
    return map_to_pixel(extent, lon, lat, size)


def render_radius_path(extent: Extent, lat: float, lon: float, radius_miles: float, size: int) -> list[tuple[float, float]]:
    points = []
    for degrees in range(0, 361, 3):
        angle = math.radians(degrees)
        d_lat = math.sin(angle) * radius_miles / 69.0
        d_lon = math.cos(angle) * radius_miles / (69.0 * max(math.cos(math.radians(lat)), 0.15))
        p_lat = lat + d_lat
        p_lon = lon + d_lon
        if extent.wkid == WEB_MERCATOR_WKID:
            points.append(latlon_to_pixel_web_mercator(extent, p_lat, p_lon, size))
        else:
            points.append(latlon_to_pixel_latlon(extent, p_lat, p_lon, size))
    return points


def fetch_layer_images(variant: Variant, size: int, latest_time_ms: int | None) -> tuple[Image.Image, Image.Image]:
    common = {
        "bbox": variant.extent.bbox_param(),
        "bboxSR": variant.extent.wkid,
        "imageSR": variant.extent.wkid,
        "size": f"{size},{size}",
        "format": "png32",
        "f": "image",
    }
    radar_params = {
        **common,
        "transparent": "true",
        "adjustAspectRatio": "false",
    }
    if variant.explicit_time and latest_time_ms is not None:
        radar_params["time"] = latest_time_ms

    basemap_params = {
        **common,
        "transparent": "false",
    }

    radar = Image.open(BytesIO(http_get(build_url(RADAR_EXPORT, radar_params)))).convert("RGBA")
    basemap = Image.open(BytesIO(http_get(build_url(BASEMAP_EXPORT, basemap_params)))).convert("RGBA")
    return basemap, radar


def radar_pixel_bbox(radar: Image.Image, extent: Extent) -> dict[str, object]:
    alpha = radar.getchannel("A")
    bbox = alpha.point(lambda value: 255 if value > 16 else 0).getbbox()
    if not bbox:
        return {"has_pixels": False}
    left, top, right, bottom = bbox
    return {
        "has_pixels": True,
        "pixel_bbox": [left, top, right, bottom],
        "extent_bbox": [
            extent.min_x + (left / radar.width) * extent.width,
            extent.max_y - (bottom / radar.height) * extent.height,
            extent.min_x + (right / radar.width) * extent.width,
            extent.max_y - (top / radar.height) * extent.height,
        ],
    }


def draw_debug_overlay(image: Image.Image, variant: Variant, lat: float, lon: float, size: int) -> Image.Image:
    output = image.copy()
    draw = ImageDraw.Draw(output)
    if variant.extent.wkid == WEB_MERCATOR_WKID:
        center = latlon_to_pixel_web_mercator(variant.extent, lat, lon, size)
    else:
        center = latlon_to_pixel_latlon(variant.extent, lat, lon, size)

    circle = render_radius_path(variant.extent, lat, lon, variant.radius_miles_for_circle, size)
    draw.line(circle, fill=(230, 40, 40, 235), width=5, joint="curve")
    cx, cy = center
    draw.line([(cx - 16, cy), (cx + 16, cy)], fill=(230, 40, 40, 255), width=5)
    draw.line([(cx, cy - 16), (cx, cy + 16)], fill=(230, 40, 40, 255), width=5)
    draw.ellipse((cx - 7, cy - 7, cx + 7, cy + 7), fill=(230, 40, 40, 255))
    return output


def label_image(image: Image.Image, title: str, subtitle: str = "") -> Image.Image:
    font = ImageFont.load_default(size=24)
    small = ImageFont.load_default(size=16)
    label_h = 74
    output = Image.new("RGB", (image.width, image.height + label_h), (250, 250, 246))
    output.paste(image.convert("RGB"), (0, label_h))
    draw = ImageDraw.Draw(output)
    draw.text((12, 10), title, fill=(20, 24, 28), font=font)
    if subtitle:
        draw.text((12, 42), subtitle, fill=(80, 80, 84), font=small)
    return output


def make_contact_sheet(images: Iterable[Image.Image], columns: int = 2, gutter: int = 24) -> Image.Image:
    images = list(images)
    cell_w = max(image.width for image in images)
    cell_h = max(image.height for image in images)
    rows = math.ceil(len(images) / columns)
    sheet = Image.new("RGB", (columns * cell_w + (columns + 1) * gutter, rows * cell_h + (rows + 1) * gutter), (246, 246, 240))
    for index, image in enumerate(images):
        row, col = divmod(index, columns)
        x = gutter + col * (cell_w + gutter)
        y = gutter + row * (cell_h + gutter)
        sheet.paste(image, (x, y))
    return sheet


def save_png(path: Path, image: Image.Image) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, "PNG")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--lat", type=float, default=YPSILANTI_LAT)
    parser.add_argument("--lon", type=float, default=YPSILANTI_LON)
    parser.add_argument("--radius", type=float, default=75.0)
    parser.add_argument("--size", type=int, default=1024)
    parser.add_argument("--out", type=Path, default=Path("artifacts/radar-debug/ypsilanti"))
    args = parser.parse_args()

    args.out.mkdir(parents=True, exist_ok=True)
    metadata = fetch_json(f"{RADAR_SERVICE}?f=json")
    latest_time_ms = metadata.get("timeInfo", {}).get("timeExtent", [None, None])[1]
    latest_iso = (
        datetime.fromtimestamp(latest_time_ms / 1000, tz=timezone.utc).isoformat()
        if latest_time_ms
        else "unknown"
    )

    variants = [
        Variant(
            name="candidate_75mi_wide_latest_time",
            description="Proposed app view: about 75 miles across, explicit latest radar time",
            extent=web_mercator_extent(args.lat, args.lon, args.radius / 2),
            explicit_time=True,
            radius_miles_for_circle=args.radius / 2,
        ),
        Variant(
            name="old_75mi_radius_default_time",
            description="Old app behavior: 75-mile radius, no explicit time",
            extent=web_mercator_extent(args.lat, args.lon, args.radius),
            explicit_time=False,
            radius_miles_for_circle=args.radius,
        ),
        Variant(
            name="old_75mi_radius_latest_time",
            description="Old scale with explicit latest service time",
            extent=web_mercator_extent(args.lat, args.lon, args.radius),
            explicit_time=True,
            radius_miles_for_circle=args.radius,
        ),
        Variant(
            name="latlon_4326_75mi_radius_latest_time",
            description="Old 75-mile radius scale with lat/lon export comparison",
            extent=latlon_extent(args.lat, args.lon, args.radius),
            explicit_time=True,
            radius_miles_for_circle=args.radius,
        ),
    ]

    report: dict[str, object] = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "location": {"label": "Ypsilanti, MI", "lat": args.lat, "lon": args.lon},
        "requestedRadiusMiles": args.radius,
        "latestRadarTimeUtc": latest_iso,
        "variants": [],
    }
    sheets: list[Image.Image] = []

    for variant in variants:
        print(f"Fetching {variant.name}...")
        basemap, radar = fetch_layer_images(variant, args.size, latest_time_ms)
        composite = Image.alpha_composite(basemap, radar)
        debug = draw_debug_overlay(composite, variant, args.lat, args.lon, args.size)
        radar_debug = draw_debug_overlay(Image.new("RGBA", radar.size, (245, 245, 245, 255)), variant, args.lat, args.lon, args.size)
        radar_debug = Image.alpha_composite(radar_debug, radar)

        variant_dir = args.out / variant.name
        save_png(variant_dir / "basemap.png", basemap)
        save_png(variant_dir / "radar.png", radar)
        save_png(variant_dir / "composite.png", composite)
        save_png(variant_dir / "debug_overlay.png", debug)
        save_png(variant_dir / "radar_only_debug_overlay.png", radar_debug)

        bbox_info = radar_pixel_bbox(radar, variant.extent)
        report["variants"].append(
            {
                "name": variant.name,
                "description": variant.description,
                "spatialReference": variant.extent.wkid,
                "extent": {
                    "minX": variant.extent.min_x,
                    "minY": variant.extent.min_y,
                    "maxX": variant.extent.max_x,
                    "maxY": variant.extent.max_y,
                },
                "explicitTime": variant.explicit_time,
                "radarPixels": bbox_info,
            }
        )

        sheets.extend(
            [
                label_image(debug, variant.name, variant.description),
                label_image(radar_debug.convert("RGB"), f"{variant.name} radar-only", f"latest={latest_iso}"),
            ]
        )
        time.sleep(0.25)

    contact_sheet = make_contact_sheet(sheets, columns=2)
    save_png(args.out / "radar_debug_contact_sheet.png", contact_sheet)
    (args.out / "radar_debug_report.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(f"Wrote {args.out / 'radar_debug_contact_sheet.png'}")
    print(f"Wrote {args.out / 'radar_debug_report.json'}")


if __name__ == "__main__":
    main()
