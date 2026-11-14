const bindings = new WeakMap();
// Touch navigation supplements the visible tabs. Vertical scrolling, zoom and
// browser edge gestures retain priority; reaching the last tab never wraps.
export function bindSwipeTabs(root, selector) {
  bindings.get(root)?.abort();
  const controller = new AbortController();
  bindings.set(root, controller);
  const options = { signal: controller.signal };
  const tabs = [...root.querySelectorAll(selector)];
  let gesture = null;
  let suppressClick = false;
  const change = direction => {
    const current = tabs.findIndex(tab => tab.getAttribute('aria-selected') === 'true');
    const next = tabs[current + direction];
    if (next) { suppressClick = false; next.click(); }
  };
  root.addEventListener('touchstart', event => {
    gesture = null;
    suppressClick = false;
    if (!tabs[0]?.isConnected || event.touches.length !== 1 || event.target.closest('input,select,textarea,a,[contenteditable="true"],[role="slider"],dialog')) return;
    const touch = event.touches[0];
    if (touch.clientX < 24 || touch.clientX > innerWidth - 24) return;
    for (let node = event.target; node && node !== root; node = node.parentElement) {
      if (node.scrollWidth > node.clientWidth + 2 && /auto|scroll/.test(getComputedStyle(node).overflowX)) return;
    }
    gesture = { x: touch.clientX, y: touch.clientY, started: performance.now(), axis: null };
  }, { ...options, passive: true });
  root.addEventListener('touchmove', event => {
    if (!gesture) return;
    if (event.touches.length !== 1) { gesture = null; return; }
    const dx = event.touches[0].clientX - gesture.x;
    const dy = event.touches[0].clientY - gesture.y;
    if (!gesture.axis && Math.max(Math.abs(dx), Math.abs(dy)) > 12) {
      gesture.axis = Math.abs(dx) > Math.abs(dy) * 1.5 ? 'x' : 'y';
    }
    if (gesture.axis === 'x') { event.preventDefault(); suppressClick = true; }
  }, { ...options, passive: false });
  root.addEventListener('touchend', event => {
    const previous = gesture; gesture = null;
    if (!previous || previous.axis !== 'x' || event.touches.length || performance.now() - previous.started > 1000) return;
    const dx = event.changedTouches[0].clientX - previous.x;
    if (Math.abs(dx) >= 60) change(dx < 0 ? 1 : -1);
  }, options);
  root.addEventListener('touchcancel', () => { gesture = null; }, options);
  root.addEventListener('click', event => {
    if (suppressClick && event.isTrusted) { event.preventDefault(); event.stopImmediatePropagation(); suppressClick = false; }
  }, { ...options, capture: true });
  tabs.forEach((tab, index) => {
    tab.tabIndex = tab.getAttribute('aria-selected') === 'true' ? 0 : -1;
    tab.addEventListener('keydown', event => {
      const next = event.key === 'ArrowRight' ? index + 1 : event.key === 'ArrowLeft' ? index - 1 : event.key === 'Home' ? 0 : event.key === 'End' ? tabs.length - 1 : -1;
      if (next < 0 || next >= tabs.length) return;
      event.preventDefault(); tabs[next].click();
      root.querySelectorAll(selector)[next]?.focus();
    }, options);
  });
}
