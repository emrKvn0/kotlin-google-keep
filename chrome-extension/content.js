(function () {
  'use strict';

  const DEFAULT_CONFIG = {
    format: '[━━━━━━━━━━━━━━━━━━]\\n[📅 ]D MMMM dddd [•] HH:mm',
    appendNewline: true,
    smartSpacing: true,
    locale: 'tr-TR'
  };

  let currentConfig = { ...DEFAULT_CONFIG };
  let lastFocusedEditable = null;

  // Ayarları oku
  function loadConfig() {
    if (typeof chrome !== 'undefined' && chrome.storage && chrome.storage.sync) {
      chrome.storage.sync.get(DEFAULT_CONFIG, (items) => {
        if (items) {
          currentConfig = { ...DEFAULT_CONFIG, ...items };
        }
      });
    }
  }

  // Değişiklikleri anlık olarak dinle
  if (typeof chrome !== 'undefined' && chrome.storage && chrome.storage.onChanged) {
    chrome.storage.onChanged.addListener((changes, area) => {
      if (area === 'sync' || area === 'local') {
        for (const [key, val] of Object.entries(changes)) {
          currentConfig[key] = val.newValue;
        }
      }
    });
  }

  loadConfig();

  // Son odaklanılan metin kutusunu takip et
  document.addEventListener('focusin', (e) => {
    const el = e.target;
    if (el && (el.isContentEditable || el.tagName === 'TEXTAREA' || el.tagName === 'INPUT')) {
      lastFocusedEditable = el;
    }
  }, true);

  function formatDate(date, formatStr, locale) {
    if (!formatStr) formatStr = DEFAULT_CONFIG.format;
    if (!locale) locale = DEFAULT_CONFIG.locale;

    // [Sabit Metin] koruması (token harfi içermeyen yer tutucu)
    const literals = [];
    let workingStr = formatStr.replace(/\[([^\]]+)\]/g, (_, p1) => {
      literals.push(p1);
      return `\u0000${literals.length - 1}\u0000`;
    });

    const pad = (n, len = 2) => String(n).padStart(len, '0');
    const year = date.getFullYear();
    const month = date.getMonth();
    const day = date.getDate();
    const hours = date.getHours();
    const hours12 = hours % 12 || 12;
    const minutes = date.getMinutes();
    const seconds = date.getSeconds();

    const fullMonth = new Intl.DateTimeFormat(locale, { month: 'long' }).format(date);
    const shortMonth = new Intl.DateTimeFormat(locale, { month: 'short' }).format(date);
    const fullWeekday = new Intl.DateTimeFormat(locale, { weekday: 'long' }).format(date);
    const shortWeekday = new Intl.DateTimeFormat(locale, { weekday: 'short' }).format(date);
    const minWeekday = shortWeekday.slice(0, 2);

    const isPM = hours >= 12;
    const ampmUpper = locale.startsWith('tr') ? (isPM ? 'ÖS' : 'ÖÖ') : (isPM ? 'PM' : 'AM');
    const ampmLower = ampmUpper.toLowerCase();

    const map = {
      'YYYY': year,
      'YY': String(year).slice(-2),
      'MMMM': fullMonth,
      'MMM': shortMonth,
      'MM': pad(month + 1),
      'M': month + 1,
      'dddd': fullWeekday,
      'ddd': shortWeekday,
      'dd': minWeekday,
      'DD': pad(day),
      'D': day,
      'HH': pad(hours),
      'H': hours,
      'hh': pad(hours12),
      'h': hours12,
      'mm': pad(minutes),
      'm': minutes,
      'ss': pad(seconds),
      's': seconds,
      'A': ampmUpper,
      'a': ampmLower
    };

    const tokenRegex = /YYYY|YY|MMMM|MMM|MM|M|dddd|ddd|dd|DD|D|HH|H|hh|h|mm|m|ss|s|A|a/g;
    let formatted = workingStr.replace(tokenRegex, (token) => map[token] !== undefined ? map[token] : token);

    // Sabit metinleri geri koy ve \n kaçışlarını gerçek satır sonuna çevir
    formatted = formatted.replace(/\u0000(\d+)\u0000/g, (_, idx) => literals[Number(idx)] || '');
    formatted = formatted.replace(/\\n/g, '\n');
    return formatted;
  }

  function escapeHtml(str) {
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/"/g, '&quot;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
  }

  function extractTextWithBreaks(node) {
    let out = '';
    const blockTags = new Set(['DIV', 'P', 'LI', 'BLOCKQUOTE', 'H1', 'H2', 'H3', 'H4', 'H5', 'H6']);
    for (const child of node.childNodes) {
      if (child.nodeType === Node.TEXT_NODE) {
        out += child.nodeValue || '';
      } else if (child.nodeType === Node.ELEMENT_NODE) {
        if (child.tagName === 'BR') {
          out += '\n';
        } else {
          if (blockTags.has(child.tagName) && out.length > 0 && !out.endsWith('\n')) {
            out += '\n';
          }
          out += extractTextWithBreaks(child);
        }
      }
    }
    return out;
  }

  function getNeededLeadingBreaks(target) {
    if (!currentConfig.smartSpacing) return 0;
    try {
      if (!target.isContentEditable) {
        const val = target.value || '';
        const pos = typeof target.selectionStart === 'number' ? target.selectionStart : val.length;
        const pre = val.slice(0, pos);
        if (pre.trim().length === 0) return 0;
        if (pre.endsWith('\n\n')) return 0;
        return pre.endsWith('\n') ? 1 : 2;
      }

      const sel = window.getSelection();
      if (!sel || sel.rangeCount === 0) {
        return target.textContent.trim().length > 0 ? 2 : 0;
      }
      const range = sel.getRangeAt(0);
      if (!target.contains(range.startContainer)) {
        return target.textContent.trim().length > 0 ? 2 : 0;
      }

      const preRange = document.createRange();
      preRange.selectNodeContents(target);
      preRange.setEnd(range.startContainer, range.startOffset);
      const preText = extractTextWithBreaks(preRange.cloneContents());

      if (preText.trim().length === 0) return 0;
      if (preText.endsWith('\n\n')) return 0;
      return preText.endsWith('\n') ? 1 : 2;
    } catch (_) {
      return 0;
    }
  }

  function getStamp() {
    return formatDate(new Date(), currentConfig.format, currentConfig.locale);
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
    const stamp = getStamp().trim();
    const toastLabel = stamp.split('\n').map((s) => s.trim()).filter(Boolean).pop() || stamp;
    const target = (document.activeElement && (document.activeElement.isContentEditable || document.activeElement.tagName === 'TEXTAREA' || document.activeElement.tagName === 'INPUT'))
      ? document.activeElement
      : lastFocusedEditable;

    if (target && document.body.contains(target)) {
      target.focus();
      const leadingBreaks = getNeededLeadingBreaks(target);
      const textToInsert = '\n'.repeat(leadingBreaks) + stamp + (currentConfig.appendNewline ? '\n' : '');

      // Tek seferde \n içeren insertText kullanımı Blink contentEditable'da satırları doğru ayırır
      const success = document.execCommand('insertText', false, textToInsert);
      if (!success) {
        if (target.isContentEditable) {
          const sel = window.getSelection();
          if (sel && sel.rangeCount > 0) {
            const range = sel.getRangeAt(0);
            range.deleteContents();
            range.insertNode(document.createTextNode(textToInsert));
            range.collapse(false);
          } else {
            target.textContent += textToInsert;
          }
        } else {
          const start = typeof target.selectionStart === 'number' ? target.selectionStart : target.value.length;
          const end = typeof target.selectionEnd === 'number' ? target.selectionEnd : start;
          target.value = target.value.slice(0, start) + textToInsert + target.value.slice(end);
          target.selectionStart = target.selectionEnd = start + textToInsert.length;
        }
      }

      target.dispatchEvent(new Event('input', { bubbles: true }));
      showToast(`Eklendi: ${toastLabel}`);
    } else {
      navigator.clipboard.writeText(stamp + (currentConfig.appendNewline ? '\n' : '')).then(() => {
        showToast(`Açık not bulunamadı, panoya kopyalandı: ${toastLabel}`);
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

  // Keep içi ayar modalı
  function openSettingsModal() {
    let modal = document.querySelector('.keep-date-modal-overlay');
    if (modal) {
      modal.remove();
    }

    modal = document.createElement('div');
    modal.className = 'keep-date-modal-overlay';
    modal.innerHTML = `
      <div class="keep-date-modal-card">
        <div class="keep-date-modal-header">
          <h3>📅 Tarih Formatı Ayarları</h3>
          <button class="keep-date-modal-close" type="button">&times;</button>
        </div>
        <div class="keep-date-modal-body">
          <label class="keep-date-label">Format Şablonu (Alt satır için \\n kullanabilirsiniz):</label>
          <input type="text" class="keep-date-modal-input" id="keepModalFormat" value="${escapeHtml(currentConfig.format || '')}" placeholder="Örn: [━━━━━━━━━━━━━━━━━━]\\n[📅 ]D MMMM dddd [•] HH:mm" />
          
          <div class="keep-date-preview-wrapper">
            <span class="keep-date-preview-label">Önizleme:</span>
            <span id="keepModalPreview" class="keep-date-preview-val"></span>
          </div>

          <div class="keep-date-presets">
            <span class="keep-date-sub">Hızlı Şablonlar:</span>
            <div class="keep-date-preset-chips">
              <button type="button" data-val="[━━━━━━━━━━━━━━━━━━]\\n[📅 ]D MMMM dddd [•] HH:mm">━━ Çizgili Başlık (Önerilen)</button>
              <button type="button" data-val="[━━ ]D MMMM dddd [•] HH:mm[ ━━]">━━ Tek Satır Çizgili ━━</button>
              <button type="button" data-val="DD.MM.YYYY dddd HH:mm">04.11.2026 Çarşamba 18:30</button>
              <button type="button" data-val="D MMMM YYYY dddd, HH:mm">4 Kasım 2026 Çarşamba, 18:30</button>
              <button type="button" data-val="DD.MM.YYYY HH:mm">04.11.2026 18:30</button>
            </div>
          </div>

          <label class="keep-date-checkbox-label">
            <input type="checkbox" id="keepModalSmartSpacing" ${currentConfig.smartSpacing ? 'checked' : ''} />
            <span>Üstüne boş satır bırak (alt alta notlar için)</span>
          </label>

          <label class="keep-date-checkbox-label">
            <input type="checkbox" id="keepModalNewline" ${currentConfig.appendNewline ? 'checked' : ''} />
            <span>Ekledikten sonra alt satıra geç</span>
          </label>
        </div>
        <div class="keep-date-modal-footer">
          <button type="button" class="keep-date-btn-save">Kaydet</button>
        </div>
      </div>
    `;

    document.body.appendChild(modal);

    const input = modal.querySelector('#keepModalFormat');
    const preview = modal.querySelector('#keepModalPreview');
    const smartSpacingCheck = modal.querySelector('#keepModalSmartSpacing');
    const newlineCheck = modal.querySelector('#keepModalNewline');
    const closeBtn = modal.querySelector('.keep-date-modal-close');
    const saveBtn = modal.querySelector('.keep-date-btn-save');
    const chips = modal.querySelectorAll('.keep-date-preset-chips button');

    function updateModalPreview() {
      preview.textContent = formatDate(new Date(), input.value || DEFAULT_CONFIG.format, currentConfig.locale);
    }

    updateModalPreview();
    input.addEventListener('input', updateModalPreview);

    chips.forEach(b => {
      b.addEventListener('click', () => {
        input.value = b.getAttribute('data-val');
        updateModalPreview();
      });
    });

    closeBtn.addEventListener('click', () => modal.remove());
    modal.addEventListener('click', (e) => {
      if (e.target === modal) modal.remove();
    });

    saveBtn.addEventListener('click', () => {
      const newFormat = input.value.trim() || DEFAULT_CONFIG.format;
      const newSmartSpacing = smartSpacingCheck.checked;
      const newNewline = newlineCheck.checked;
      currentConfig.format = newFormat;
      currentConfig.smartSpacing = newSmartSpacing;
      currentConfig.appendNewline = newNewline;

      if (typeof chrome !== 'undefined' && chrome.storage && chrome.storage.sync) {
        chrome.storage.sync.set({
          format: newFormat,
          smartSpacing: newSmartSpacing,
          appendNewline: newNewline
        }, () => {
          showToast('Tarih formatı güncellendi ✓');
          modal.remove();
        });
      } else {
        modal.remove();
      }
    });
  }

  // Yüzen buton konteyneri
  function createFab() {
    if (document.querySelector('.keep-date-fab-container')) return;
    const container = document.createElement('div');
    container.className = 'keep-date-fab-container';

    // Ana ekleme butonu
    const btn = document.createElement('button');
    btn.className = 'keep-date-fab';
    btn.type = 'button';
    btn.title = 'Aktif nota tarih/saat ekle (Kısayol: Alt + D)';
    btn.innerHTML = '<span>📅</span><span>Tarih Ekle</span>';

    btn.addEventListener('mousedown', (e) => {
      e.preventDefault();
      insertDateStamp();
    });

    // Hızlı ayar butonu
    const settingsBtn = document.createElement('button');
    settingsBtn.className = 'keep-date-fab-settings';
    settingsBtn.type = 'button';
    settingsBtn.title = 'Formatı Değiştir';
    settingsBtn.innerHTML = '⚙️';

    settingsBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      openSettingsModal();
    });

    container.appendChild(settingsBtn);
    container.appendChild(btn);
    document.body.appendChild(container);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', createFab);
  } else {
    createFab();
  }
})();
