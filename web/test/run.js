const fs = require("fs");
const { JSDOM } = require("jsdom");

const html = fs.readFileSync("/scratch/work/gal/web/index.html", "utf8");
const problems = [];

const dom = new JSDOM(html, {
  runScripts: "dangerously",
  pretendToBeVisual: true,
  url: "https://example.test/gallery/",
  beforeParse(window) {
    // jsdom has no object URLs and no media decoding; both are irrelevant to the logic.
    let n = 0;
    window.URL.createObjectURL = () => "blob:stub/" + ++n;
    window.URL.revokeObjectURL = () => {};
    window.matchMedia = window.matchMedia || (() => ({
      matches: false,
      addEventListener() {},
      removeEventListener() {},
    }));
    window.addEventListener("error", (e) => problems.push("window error: " + e.message));
  },
});

const { window } = dom;
const doc = window.document;

function check(name, condition, extra) {
  console.log((condition ? "ok   " : "FAIL ") + name + (extra !== undefined ? "  → " + extra : ""));
  if (!condition) problems.push(name);
}

function makeFile(name, size, type, when, path) {
  const file = new window.File([new Uint8Array(size)], name, { type, lastModified: when });
  Object.defineProperty(file, "webkitRelativePath", { value: path });
  return file;
}

const now = Date.now();
const files = [
  makeFile("beach.jpg", 1200, "image/jpeg", now - 1000, "Camera/beach.jpg"),
  makeFile("palm.jpg", 3400, "image/jpeg", now - 5000, "Camera/palm.jpg"),
  makeFile("clip.mp4", 9000, "video/mp4", now - 900, "Camera/clip.mp4"),
  makeFile("screenshot.png", 700, "image/png", now - 90000, "Screenshots/screenshot.png"),
  makeFile(".hidden.jpg", 100, "image/jpeg", now, "Camera/.hidden.jpg"),
  makeFile("notes.txt", 100, "text/plain", now, "Camera/notes.txt"),
];

check("empty state shown before any files", doc.getElementById("empty").style.display !== "none");
check("count says nothing is open", doc.getElementById("count").textContent === "No folder open",
  doc.getElementById("count").textContent);

window.NijatvaGallery.addFiles(files, "Camera");
window.NijatvaGallery.render();

const cells = doc.querySelectorAll(".cell").length;
check("four media files land in the grid (txt and dotfile skipped)", cells === 4, cells);

const count = doc.getElementById("count").textContent;
check("count line reads items and size", /^4 items · 14 KB$/.test(count), count);

const firstCellName = doc.querySelector(".cell").getAttribute("title") || "";
const order = Array.from(doc.querySelectorAll(".cell")).length;
check("grid renders every item", order === 4, order);

check("video cell carries a play glyph", !!doc.querySelector(".cell .play"));

// folders tab
doc.getElementById("tabFolders").click();
const folderCards = doc.querySelectorAll(".folder").length;
check("folders tab shows one card per folder", folderCards === 2, folderCards);
const firstMeta = doc.querySelector(".folder .meta").textContent;
check("folder card shows count and size", /3 items · 13\.3 KB/.test(firstMeta), firstMeta);

// back to photos, open the viewer
doc.getElementById("tabPhotos").click();
doc.querySelector(".cell").click();
check("viewer opens", doc.getElementById("viewer").classList.contains("open"));
check("counter starts at 1", doc.getElementById("vCounter").textContent === "1 / 4",
  doc.getElementById("vCounter").textContent);

// details
doc.getElementById("vInfo").click();
const rows = Array.from(doc.querySelectorAll("#infoRows .row .k")).map((n) => n.textContent);
check("details sheet lists the facts", rows.includes("Name") && rows.includes("Size") && rows.includes("Taken"),
  rows.join(", "));

// paging
window.document.dispatchEvent(new window.KeyboardEvent("keydown", { key: "ArrowRight", bubbles: true }));
check("arrow right pages forward", doc.getElementById("vCounter").textContent === "2 / 4",
  doc.getElementById("vCounter").textContent);

window.document.dispatchEvent(new window.KeyboardEvent("keydown", { key: "Escape", bubbles: true }));
check("escape closes the viewer", !doc.getElementById("viewer").classList.contains("open"));

// sort cycle: recent → oldest
doc.getElementById("sortChip").click();
check("sort chip cycles", doc.getElementById("sortChip").textContent === "Oldest",
  doc.getElementById("sortChip").textContent);

// appearance
doc.getElementById("appearanceBtn").click();
check("appearance sheet opens", doc.getElementById("appearance").classList.contains("open"));
const themePills = doc.querySelectorAll("#themePills .pill");
const accentPills = doc.querySelectorAll("#accentPills .pill");
check("four themes and three accents offered", themePills.length === 4 && accentPills.length === 3,
  themePills.length + "/" + accentPills.length);
themePills[2].click();
accentPills[2].click();
check("theme applied", doc.body.dataset.theme === "light", doc.body.dataset.theme);
check("accent applied", doc.body.dataset.accent === "indigo", doc.body.dataset.accent);
check("appearance persisted", JSON.parse(window.localStorage.getItem("nijatva.gallery.appearance")).accent === "indigo");

console.log("\nproblems:", problems.length ? problems : "none");
process.exit(problems.length ? 1 : 0);
