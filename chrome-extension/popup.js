(function () {
  'use strict';

  const DEFAULT_CONFIG = {
    format: '[━━━━━━━━━━━━━━━━━━]\\n[📅 ]D MMMM dddd [•] HH:mm',
    appendNewline: true,
    smartSpacing: true,
    locale: 'tr-TR'
  };

  const formatInput = document.getElementById('formatInput');
  const previewText = document.getElementById('previewText');
  const smartSpacingCheckbox = document.getElementById('smartSpacingCheckbox');
  const newlineCheckbox = document.getElementById('newlineCheckbox');
  const localeSelect = document.getElementById('localeSelect');
  const saveBtn = document.getElementById('saveBtn');
  const statusMsg = document.getElementById('statusMsg');
  const presetButtons = document.querySelectorAll('.preset-btn');

  function formatDate(date, formatStr, locale) {
    if (!formatStr) return '';

    // Köşeli parantez içindeki literal metinleri koru (token harfi içermeyen yer tutucu)
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

    // Literal metinleri geri koy ve \n kaçışlarını gerçek satır sonuna çevir
    formatted = formatted.replace(/\u0000(\d+)\u0000/g, (_, idx) => literals[Number(idx)] || '');
    formatted = formatted.replace(/\\n/g, '\n');
    return formatted;
  }

  function updatePreview() {
    const format = formatInput.value || DEFAULT_CONFIG.format;
    const locale = localeSelect.value || DEFAULT_CONFIG.locale;
    const result = formatDate(new Date(), format, locale);
    previewText.textContent = result || '(boş)';
  }

  // Ayarları yükle
  chrome.storage.sync.get(DEFAULT_CONFIG, (items) => {
    formatInput.value = items.format || DEFAULT_CONFIG.format;
    smartSpacingCheckbox.checked = items.smartSpacing !== undefined ? items.smartSpacing : DEFAULT_CONFIG.smartSpacing;
    newlineCheckbox.checked = items.appendNewline !== undefined ? items.appendNewline : DEFAULT_CONFIG.appendNewline;
    localeSelect.value = items.locale || DEFAULT_CONFIG.locale;
    updatePreview();
  });

  // Giriş değişikliklerinde canlı güncelle
  formatInput.addEventListener('input', updatePreview);
  localeSelect.addEventListener('change', updatePreview);

  // Şablon butonları
  presetButtons.forEach((btn) => {
    btn.addEventListener('click', () => {
      formatInput.value = btn.dataset.preset;
      updatePreview();
    });
  });

  // Kaydet
  saveBtn.addEventListener('click', () => {
    const config = {
      format: formatInput.value.trim() || DEFAULT_CONFIG.format,
      smartSpacing: smartSpacingCheckbox.checked,
      appendNewline: newlineCheckbox.checked,
      locale: localeSelect.value
    };

    chrome.storage.sync.set(config, () => {
      statusMsg.textContent = 'Kaydedildi ✓';
      setTimeout(() => {
        statusMsg.textContent = '';
      }, 2000);
    });
  });
})();
