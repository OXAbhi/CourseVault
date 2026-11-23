(function () {
  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

  // Mobile sidebar
  const sidebar = $(".sidebar");
  const toggle = $(".menu-toggle");
  const backdrop = $(".backdrop");
  const openNav = (open) => {
    if (!sidebar) return;
    sidebar.classList.toggle("is-open", open);
    if (backdrop) backdrop.style.display = open ? "block" : "none";
  };
  toggle?.addEventListener("click", () => openNav(!sidebar.classList.contains("is-open")));
  backdrop?.addEventListener("click", () => openNav(false));

  // Tabs
  $$("[data-tabs]").forEach((group) => {
    const tabs = $$(".tab", group);
    const panels = $$(".tab-panel", group.closest("[data-tabset]") || document);
    tabs.forEach((tab) => {
      tab.addEventListener("click", () => {
        tabs.forEach((t) => t.classList.toggle("is-active", t === tab));
        const id = tab.dataset.tab;
        $$((group.closest("[data-tabset]") || document).querySelectorAll(".tab-panel")).forEach((p) => {
          p.classList.toggle("is-active", p.dataset.panel === id);
        });
      });
    });
  });

  // Login / register toggle
  $$("[data-auth-toggle]").forEach((btn) => {
    btn.addEventListener("click", () => {
      $$("[data-auth-toggle]").forEach((b) => b.classList.toggle("is-active", b === btn));
      const target = btn.dataset.authToggle;
      $$(".auth-form-panel").forEach((p) => p.classList.toggle("is-active", p.dataset.auth === target));
    });
  });

  const goRole = (role) => {
    const map = {
      student: "student-dashboard.html",
      teacher: "teacher-dashboard.html",
      admin: "admin-dashboard.html",
    };
    window.location.href = map[role] || "student-dashboard.html";
  };

  $("#login-form")?.addEventListener("submit", (e) => {
    e.preventDefault();
    const email = ($("#login-email")?.value || "").toLowerCase();
    if (email.includes("admin")) goRole("admin");
    else if (email.includes("iyer") || email.includes("teacher") || email.includes("prof")) goRole("teacher");
    else goRole("student");
  });

  $("#register-form")?.addEventListener("submit", (e) => {
    e.preventDefault();
    const role = $("#register-role")?.value || "student";
    goRole(role);
  });

  $$("[data-demo-role]").forEach((btn) => {
    btn.addEventListener("click", () => goRole(btn.dataset.demoRole));
  });

  // Cloudinary upload mock
  const zone = $(".dropzone");
  if (zone) {
    const input = $("#file-input");
    const fill = zone.querySelector(".progress-fill");
    const pct = zone.querySelector("[data-progress-pct]");
    const label = zone.querySelector("[data-progress-label]");
    const fileLine = zone.querySelector("[data-progress-file]");
    const cloudTag = zone.querySelector("[data-cloud-tag]");

    const runUpload = (fileName, startAt) => {
      zone.classList.remove("is-done");
      zone.classList.add("is-uploading");
      label.textContent = "Uploading to Cloudinary…";
      fileLine.textContent = fileName;
      cloudTag.textContent = "";
      let n = typeof startAt === "number" ? startAt : 0;
      fill.style.width = n + "%";
      pct.textContent = n + "%";
      const tick = () => {
        n = Math.min(100, n + Math.random() * 14 + 4);
        const shown = Math.round(n);
        fill.style.width = shown + "%";
        pct.textContent = shown + "%";
        if (n < 100) {
          setTimeout(tick, 180);
        } else {
          zone.classList.remove("is-uploading");
          zone.classList.add("is-done");
          label.textContent = "Uploaded";
          const id = "nit-nagpur/cs301/" + fileName.replace(/\s+/g, "-").replace(/\.[^.]+$/, "").toLowerCase();
          cloudTag.textContent = "public_id: " + id + "  ·  resource_type: " + (/\.(mp4|mov|webm)$/i.test(fileName) ? "video" : "raw");
        }
      };
      setTimeout(tick, 200);
    };

    const onFiles = (files) => {
      const f = files && files[0];
      if (!f) return;
      runUpload(f.name, 0);
    };

    zone.addEventListener("click", (e) => {
      if (e.target.closest("button, a")) return;
      input?.click();
    });
    input?.addEventListener("change", () => onFiles(input.files));
    ["dragenter", "dragover"].forEach((ev) => {
      zone.addEventListener(ev, (e) => {
        e.preventDefault();
        zone.classList.add("is-dragover");
      });
    });
    ["dragleave", "drop"].forEach((ev) => {
      zone.addEventListener(ev, (e) => {
        e.preventDefault();
        zone.classList.remove("is-dragover");
        if (ev === "drop") onFiles(e.dataTransfer.files);
      });
    });

    // Replay from the static mid-upload screenshot state
    $("#replay-upload")?.addEventListener("click", (e) => {
      e.stopPropagation();
      runUpload("Lecture-12-Graph-Traversals.mp4", 0);
    });
  }

  // Grading save
  const toast = $(".toast");
  const showToast = (msg) => {
    if (!toast) return;
    toast.textContent = msg;
    toast.classList.add("is-on");
    setTimeout(() => toast.classList.remove("is-on"), 2200);
  };
  $$("[data-save-grade]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const row = btn.closest("tr");
      const marks = row?.querySelector(".marks-input")?.value;
      const badge = row?.querySelector("[data-status]");
      if (badge) {
        badge.className = "badge badge-success";
        badge.textContent = "Graded";
      }
      showToast("Grade saved — " + (marks || "0") + " marks. Priya will get an in-app notification.");
    });
  });

  $$("[data-approve]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const row = btn.closest("tr");
      const badge = row?.querySelector("[data-status]");
      if (badge) {
        badge.className = "badge badge-success";
        badge.textContent = "Active";
      }
      btn.closest(".row-actions")?.querySelectorAll("button").forEach((b) => b.remove());
      showToast("Teacher account approved. They can sign in with their issued credentials.");
    });
  });
})();
