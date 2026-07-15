const apkPath = 'downloads/drift-realista-rc.apk';
const button = document.querySelector('#downloadButton');
const note = document.querySelector('#downloadNote');

fetch(apkPath, { method: 'HEAD' })
  .then((response) => {
    if (!response.ok) throw new Error('APK não encontrado');
    const size = Number(response.headers.get('content-length') || 0);
    if (size > 0) {
      note.textContent = `APK disponível para download (${(size / 1024 / 1024).toFixed(1)} MB).`;
    } else {
      note.textContent = 'APK disponível para download.';
    }
  })
  .catch(() => {
    button.classList.add('is-disabled');
    button.setAttribute('aria-disabled', 'true');
    note.innerHTML = 'APK ainda não encontrado. Gere o APK e salve em <code>site/downloads/drift-realista-rc.apk</code>.';
  });
