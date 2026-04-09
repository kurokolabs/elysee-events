/* ── Inline Status Update (global) ───────────────────── */
function updateStatus(sel) {
  var url = sel.dataset.url;
  var status = sel.value;
  var form = document.createElement('form');
  form.method = 'POST';
  form.action = url;
  form.style.display = 'none';
  var input = document.createElement('input');
  input.name = 'status';
  input.value = status;
  form.appendChild(input);
  // CSRF Token aus existierendem Formular oder meta-Tag lesen
  var csrfInput = document.querySelector('input[name="_csrf"]');
  var csrfMeta = document.querySelector('meta[name="_csrf"]');
  var csrfVal = csrfInput ? csrfInput.value : (csrfMeta ? csrfMeta.content : null);
  if (csrfVal) {
    var ci = document.createElement('input');
    ci.name = '_csrf';
    ci.value = csrfVal;
    form.appendChild(ci);
  }
  document.body.appendChild(form);
  form.submit();
}

document.addEventListener('DOMContentLoaded', function() {

  /* ── Mobile Burger Menu ─────────────────────────────────── */
  var burger = document.getElementById('portalBurger') || document.getElementById('customerBurger');
  var mobileMenu = document.getElementById('portalMobileMenu') || document.getElementById('customerMobileMenu');
  if (burger && mobileMenu) {
    burger.addEventListener('click', function() {
      burger.classList.toggle('is-open');
      mobileMenu.classList.toggle('is-open');
      burger.setAttribute('aria-label', mobileMenu.classList.contains('is-open') ? 'Menü schließen' : 'Menü öffnen');
    });
  }

  /* ── Auto-hide alerts ─────────────────────────────────── */
  document.querySelectorAll('.alert').forEach(function(alert) {
    setTimeout(function() {
      alert.style.transition = 'opacity .4s ease, transform .4s ease';
      alert.style.opacity = '0';
      alert.style.transform = 'translateY(-8px)';
      setTimeout(function() { alert.remove(); }, 400);
    }, 5000);
  });

  /* ── Scroll reveal for cards and sections ──────────────── */
  var revealElements = document.querySelectorAll(
    '.stat-card, .booking-card, .detail-card, .table-wrap, .empty-state, .temp-password'
  );

  if (revealElements.length > 0 && 'IntersectionObserver' in window) {
    revealElements.forEach(function(el, i) {
      el.style.opacity = '0';
      el.style.transform = 'translateY(20px)';
      el.style.transition = 'opacity .6s cubic-bezier(0.22,1,0.36,1), transform .6s cubic-bezier(0.22,1,0.36,1)';
      el.style.transitionDelay = (i % 6) * 0.07 + 's';
    });

    var observer = new IntersectionObserver(function(entries) {
      entries.forEach(function(entry) {
        if (entry.isIntersecting) {
          entry.target.style.opacity = '1';
          entry.target.style.transform = 'translateY(0)';
          observer.unobserve(entry.target);
        }
      });
    }, { threshold: 0.08 });

    revealElements.forEach(function(el) { observer.observe(el); });
  }

  /* ── Clickable table rows ─────────────────────────────── */
  document.querySelectorAll('.data-table tbody tr[data-href]').forEach(function(row) {
    row.style.transition = 'background .2s ease';
    row.addEventListener('click', function() {
      window.location.href = this.dataset.href;
    });
  });

  /* ── Form input animation on focus ────────────────────── */
  document.querySelectorAll('.form-input, .form-select, .form-textarea').forEach(function(input) {
    input.addEventListener('focus', function() {
      this.parentElement.style.transform = 'translateY(-1px)';
      this.parentElement.style.transition = 'transform .25s cubic-bezier(0.22,1,0.36,1)';
    });
    input.addEventListener('blur', function() {
      this.parentElement.style.transform = 'translateY(0)';
    });
  });

  /* ── Prevent double submit ──────────────────────────── */
  document.querySelectorAll('form').forEach(function(form) {
    var submitted = false;
    form.addEventListener('submit', function(e) {
      if (submitted) { e.preventDefault(); return; }
      submitted = true;
      var btns = form.querySelectorAll('button[type="submit"], .btn--gold');
      btns.forEach(function(btn) {
        btn.disabled = true;
        btn.style.opacity = '0.6';
        btn.style.pointerEvents = 'none';
      });
      setTimeout(function() {
        submitted = false;
        btns.forEach(function(btn) { btn.disabled = false; btn.style.opacity = ''; btn.style.pointerEvents = ''; });
      }, 15000);
    });
  });

});
