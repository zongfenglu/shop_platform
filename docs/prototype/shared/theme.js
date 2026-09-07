/* ============================================================
   商户后台 · 主题切换器逻辑
   四套主题：极简白(white，默认) / 翡翠绿(jade) / 石墨深(graphite) / 海洋深(ocean)
   持久化：localStorage，key = shop_theme
   ============================================================ */

(function () {
  var THEMES = [
    { id: 'white',    name: '极简白', desc: '默认外观，清爽简洁', swatch: 'theme-swatch-white' },
    { id: 'jade',     name: '翡翠绿', desc: '沉稳专业，浅色底', swatch: 'theme-swatch-jade' },
    { id: 'graphite', name: '石墨深', desc: '深色中性灰，护眼', swatch: 'theme-swatch-graphite' },
    { id: 'ocean',    name: '海洋深', desc: '深海蓝调，高级感', swatch: 'theme-swatch-ocean' }
  ];
  var STORAGE_KEY = 'shop_theme';

  function getTheme() {
    try { return localStorage.getItem(STORAGE_KEY) || 'white'; } catch (e) { return 'white'; }
  }
  function setTheme(id) {
    try { localStorage.setItem(STORAGE_KEY, id); } catch (e) {}
    applyTheme(id);
    renderPanels();
  }
  function applyTheme(id) {
    if (id === 'white') {
      document.documentElement.removeAttribute('data-theme');
    } else {
      document.documentElement.setAttribute('data-theme', id);
    }
  }

  // 尽早应用，避免主题闪烁（在 head 内联脚本中会先调用一次 applyTheme）
  applyTheme(getTheme());

  function buildSwitcher(mount) {
    var current = getTheme();
    var wrap = document.createElement('div');
    wrap.className = 'theme-switcher';

    var btn = document.createElement('div');
    btn.className = 'theme-switcher-btn';
    btn.innerHTML = '<span class="dot"></span><span class="label">主题</span>';
    btn.addEventListener('click', function (e) {
      e.stopPropagation();
      document.querySelectorAll('.theme-switcher.open').forEach(function (el) {
        if (el !== wrap) el.classList.remove('open');
      });
      wrap.classList.toggle('open');
    });

    var panel = document.createElement('div');
    panel.className = 'theme-switcher-panel';
    panel.innerHTML = '<div class="theme-switcher-title">选择后台主题</div>';
    THEMES.forEach(function (t) {
      var opt = document.createElement('div');
      opt.className = 'theme-opt' + (t.id === current ? ' active' : '');
      opt.dataset.themeId = t.id;
      opt.innerHTML =
        '<span class="swatch ' + t.swatch + '"></span>' +
        '<span class="info"><span class="name">' + t.name + '</span>' +
        '<span class="desc">' + t.desc + '</span></span>' +
        '<span class="check">✓</span>';
      opt.addEventListener('click', function (e) {
        e.stopPropagation();
        setTheme(t.id);
        wrap.classList.remove('open');
      });
      panel.appendChild(opt);
    });

    wrap.appendChild(btn);
    wrap.appendChild(panel);
    mount.appendChild(wrap);
  }

  function renderPanels() {
    var current = getTheme();
    document.querySelectorAll('.theme-switcher-panel').forEach(function (panel) {
      panel.querySelectorAll('.theme-opt').forEach(function (opt) {
        opt.classList.toggle('active', opt.dataset.themeId === current);
      });
    });
  }

  document.addEventListener('click', function () {
    document.querySelectorAll('.theme-switcher.open').forEach(function (el) {
      el.classList.remove('open');
    });
  });

  document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('[data-theme-switcher]').forEach(function (mount) {
      buildSwitcher(mount);
    });
  });
})();
