document.addEventListener('DOMContentLoaded', function() {

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

  /* ── Smooth table row highlights ──────────────────────── */
  document.querySelectorAll('.data-table tbody tr').forEach(function(row) {
    row.style.transition = 'background .2s ease';
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
    form.addEventListener('submit', function() {
      var btns = form.querySelectorAll('button[type="submit"], .btn--gold');
      btns.forEach(function(btn) {
        if (btn.disabled) return;
        btn.disabled = true;
        btn.style.opacity = '0.6';
        btn.style.pointerEvents = 'none';
        setTimeout(function() { btn.disabled = false; btn.style.opacity = ''; btn.style.pointerEvents = ''; }, 5000);
      });
    });
  });

});
