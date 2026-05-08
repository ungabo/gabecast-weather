const buttons = document.querySelectorAll("[data-view]");
const views = document.querySelectorAll(".view");

function selectView(name) {
  buttons.forEach((item) => item.classList.toggle("active", item.dataset.view === name));
  views.forEach((view) => view.classList.toggle("active", view.id === name));
}

buttons.forEach((button) => button.addEventListener("click", () => selectView(button.dataset.view)));

const requested = new URLSearchParams(window.location.search).get("view");
if (requested) selectView(requested);
