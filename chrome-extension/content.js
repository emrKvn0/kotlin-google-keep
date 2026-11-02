(function () {
  'use strict';

  let lastFocusedEditable = null;

  // Son odaklanılan metin kutusunu takip et (butona basınca odağı kaybetmemek için)
  document.addEventListener('focusin', (e) => {
    const el = e.target;
    if (el && (el.isContentEditable || el.tagName === 'TEXTAREA' || el.tagName === 'INPUT')) {
      lastFocusedEditable = el;
    }
  }, true);

  function getFormattedDate() {
    const now = new Date();
    const pad = (n) => String(n).padStart(2, '0');
    const day = pad(now.getDate());
    const month = pad(now.getMonth() + 1);
    const year = now.getFullYear();
    const hours = pad(now.getHours());
    const minutes = pad(now.getMinutes());
    return `${day}.${month}.${year} ${hours}:${minutes}\n`;
  }

  function showToast(msg) {
    let toast = document.querySelector('.keep-date-toast');
    if (!toast) {
      toast = document.createElement('div');
      toast.className = 'keep-date-toast';
      document.body.appendChild(toast);
    }
    toast.textContent = msg;
    toast.classList.add('show');
    setTimeout(() => toast.classList.remove('show'), 2200);
  }

  function insertDateStamp() {
    const stamp = getFormattedDate();
    const target = (document.activeElement && document.activeElement.isContentEditable)
      ? document.activeElement
      : lastFocusedEditable;

    if (target && document.body.contains(target)) {
      target.focus();
      // execCommand Google Keep'in contenteditable yapısıyla en uyumlu yöntemdir (Undo/Redo bozulmaz)
      const success = document.execCommand('insertText', false, stamp);
      if (!success) {
        const sel = window.getSelection();
        if (sel && sel.rangeCount > 0) {
          const range = sel.getRangeAt(0);
          range.deleteContents();
          range.insertNode(document.createTextNode(stamp));
          range.collapse(false);
        } else {
          target.textContent += stamp;
        }
        target.dispatchEvent(new Event('input', { bubbles: true }));
      }
      showToast(`Eklendi: ${stamp.trim()}`);
    } else {
      navigator.clipboard.writeText(stamp).then(() => {
        showToast(`Açık not bulunamadı, panoya kopyalandı: ${stamp.trim()}`);
      });
    }
  }

  // Alt + D kısayolu
  document.addEventListener('keydown', (e) => {
    if (e.altKey && (e.key === 'd' || e.key === 'D')) {
      e.preventDefault();
      insertDateStamp();
    }
  }, true);

  // Sağ alt yüzen buton oluştur
  function createFab() {
    if (document.querySelector('.keep-date-fab-container')) return;
    const container = document.createElement('div');
    container.className = 'keep-date-fab-container';

    const btn = document.createElement('button');
    btn.className = 'keep-date-fab';
    btn.type = 'button';
    btn.title = 'Aktif nota tarih/saat ekle (Kısayol: Alt + D)';
    btn.innerHTML = '<span>📅</span><span>Tarih Ekle (Alt+D)</span>';

    // mousedown'da preventDefault yaparak Keep notundaki imleç odağının kaybolmasını önlüyoruz!
    btn.addEventListener('mousedown', (e) => {
      e.preventDefault();
      insertDateStamp();
    });

    container.appendChild(btn);
    document.body.appendChild(container);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', createFab);
  } else {
    createFab();
  }
})();
