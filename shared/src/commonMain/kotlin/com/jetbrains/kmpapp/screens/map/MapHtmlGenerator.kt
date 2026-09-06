package com.jetbrains.kmpapp.screens.map

object MapHtmlGenerator {

    fun generateHtml(
        svgContent: String,
        isDark: Boolean = true
    ): String {
        val themeStyles = if (isDark) {
            """
            /* Official pulse maps - Dark theme */
            g[role="button"] path {
              fill: #161b22 !important;
              stroke: #484f58 !important;
              stroke-width: 6px !important;
              cursor: pointer;
              transition: fill 0.15s ease, stroke 0.15s ease;
            }
            g[style*="pointer-events: none"] > path {
              fill: #0d1117 !important;
              stroke: #30363d !important;
              stroke-width: 6px !important;
            }
            .room-label {
              fill: #f0f6fc !important;
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif !important;
              font-weight: 600 !important;
              letter-spacing: 0.5px;
              stroke: #0d1117 !important;
              stroke-width: 4.5px !important;
              paint-order: stroke fill !important;
              pointer-events: none;
            }
            #markers text {
              fill: #58a6ff !important;
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif !important;
              stroke: #0d1117 !important;
              stroke-width: 5px !important;
              paint-order: stroke fill !important;
              font-weight: 700 !important;
              pointer-events: none;
            }
            #markers rect {
              fill: #15325b !important;
              stroke: #58a6ff !important;
            }
            #markers path {
              stroke: #58a6ff !important;
            }
            .selected-room > path {
              fill: #1f6feb !important;
              stroke: #58a6ff !important;
              stroke-width: 10px !important;
            }

            /* Legacy MP-1 dark theme */
            .BigAreaPath { fill: #0d1117 !important; stroke: #30363d !important; }
            rect[fill="#F8F8F8"] { fill: #0d1117 !important; stroke: #30363d !important; }
            rect[fill="#fff"], rect[fill="#FFFFFF"] { fill: #161b22 !important; stroke: #484f58 !important; }
            path[fill="#262A34"] { fill: #e6edf3 !important; }
            path[fill="#000"], path[fill="#000000"] { fill: #cbd5e1 !important; }
            .Room:hover rect, .Room:active rect { fill: #1f6feb !important; }
            """
        } else {
            """
            /* Official pulse maps - Light theme */
            g[role="button"] path {
              fill: #ffffff !important;
              stroke: #94a3b8 !important;
              stroke-width: 6px !important;
              cursor: pointer;
              transition: fill 0.15s ease, stroke 0.15s ease;
            }
            g[style*="pointer-events: none"] > path {
              fill: #f1f5f9 !important;
              stroke: #cbd5e1 !important;
              stroke-width: 6px !important;
            }
            .room-label {
              fill: #0f172a !important;
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif !important;
              font-weight: 600 !important;
              letter-spacing: 0.5px;
              stroke: #ffffff !important;
              stroke-width: 4.5px !important;
              paint-order: stroke fill !important;
              pointer-events: none;
            }
            #markers text {
              fill: #2563eb !important;
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif !important;
              stroke: #ffffff !important;
              stroke-width: 5px !important;
              paint-order: stroke fill !important;
              font-weight: 700 !important;
              pointer-events: none;
            }
            #markers rect {
              fill: #eff6ff !important;
              stroke: #2563eb !important;
            }
            #markers path {
              stroke: #2563eb !important;
            }
            .selected-room > path {
              fill: #bfdbfe !important;
              stroke: #2563eb !important;
              stroke-width: 10px !important;
            }

            /* Legacy MP-1 light theme */
            .BigAreaPath { fill: #f1f5f9 !important; stroke: #cbd5e1 !important; }
            rect[fill="#F8F8F8"] { fill: #f8fafc !important; stroke: #cbd5e1 !important; }
            rect[fill="#fff"], rect[fill="#FFFFFF"] { fill: #ffffff !important; stroke: #cbd5e1 !important; }
            path[fill="#262A34"] { fill: #1e293b !important; }
            path[fill="#000"], path[fill="#000000"] { fill: #0f172a !important; }
            .Room:hover rect, .Room:active rect { fill: #60a5fa !important; }
            """
        }

        val cardBg = if (isDark) "rgba(22, 27, 34, 0.92)" else "rgba(255, 255, 255, 0.94)"
        val cardBorder = if (isDark) "rgba(255, 255, 255, 0.12)" else "rgba(0, 0, 0, 0.08)"
        val cardTitleColor = if (isDark) "#f0f6fc" else "#0f172a"
        val cardSubColor = if (isDark) "#8b949e" else "#64748b"

        return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=10.0, user-scalable=no">
<style>
  * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
  html, body {
    margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden;
    background-color: transparent; user-select: none; -webkit-user-select: none;
    touch-action: none;
  }
  #viewport {
    width: 100%; height: 100%; position: relative; overflow: hidden;
    cursor: grab;
    touch-action: none;
  }
  #viewport:active { cursor: grabbing; }
  #svg-container {
    position: absolute;
    transform-origin: 0 0;
    will-change: transform;
    touch-action: none;
  }
  svg {
    display: block;
    width: auto;
    height: auto;
    max-width: none;
  }
  $themeStyles
  
  .Room rect, .Room path {
    cursor: pointer;
    transition: fill 0.15s ease, stroke 0.15s ease;
  }

  /* Floating Info Card */
  .room-card {
    position: absolute;
    top: 76px;
    left: 50%;
    transform: translateX(-50%) translateY(0);
    background: $cardBg;
    backdrop-filter: blur(16px);
    -webkit-backdrop-filter: blur(16px);
    border: 1px solid $cardBorder;
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.22);
    border-radius: 18px;
    padding: 10px 16px;
    z-index: 1000;
    transition: opacity 0.22s cubic-bezier(0.2, 0, 0, 1), transform 0.22s cubic-bezier(0.2, 0, 0, 1);
    pointer-events: auto;
    max-width: 88%;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  }
  .room-card.hidden {
    opacity: 0;
    transform: translateX(-50%) translateY(-14px);
    pointer-events: none;
  }
  .room-card-content {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .room-card-icon {
    font-size: 20px;
    line-height: 1;
    flex-shrink: 0;
  }
  .room-card-text {
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }
  .room-card-title {
    font-size: 15px;
    font-weight: 700;
    color: $cardTitleColor;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .room-card-subtitle {
    font-size: 12px;
    color: $cardSubColor;
    margin-top: 1px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .room-card-close {
    background: transparent;
    border: none;
    color: $cardSubColor;
    font-size: 15px;
    font-weight: bold;
    cursor: pointer;
    padding: 4px 6px;
    margin-left: 6px;
    border-radius: 8px;
    line-height: 1;
    flex-shrink: 0;
  }
</style>
</head>
<body>
<div id="viewport">
  <div id="room-card" class="room-card hidden">
    <div class="room-card-content">
      <div class="room-card-icon" id="room-card-icon">📍</div>
      <div class="room-card-text">
        <div class="room-card-title" id="room-card-title"></div>
        <div class="room-card-subtitle" id="room-card-subtitle"></div>
      </div>
      <button class="room-card-close" id="room-card-close">✕</button>
    </div>
  </div>
  <div id="svg-container">
    $svgContent
  </div>
</div>
<script>
  const viewport = document.getElementById('viewport');
  const container = document.getElementById('svg-container');
  const roomCard = document.getElementById('room-card');
  const roomTitle = document.getElementById('room-card-title');
  const roomSubtitle = document.getElementById('room-card-subtitle');
  const roomIcon = document.getElementById('room-card-icon');
  const roomClose = document.getElementById('room-card-close');
  
  let scale = 1;
  let translateX = 0;
  let translateY = 0;

  let selectedElement = null;

  function hideCard() {
    roomCard.classList.add('hidden');
    if (selectedElement) {
      selectedElement.classList.remove('selected-room');
      selectedElement = null;
    }
  }

  roomClose.addEventListener('click', (e) => {
    e.stopPropagation();
    hideCard();
  });

  function selectRoom(el) {
    if (selectedElement) {
      selectedElement.classList.remove('selected-room');
    }
    selectedElement = el;
    el.classList.add('selected-room');

    const label = el.getAttribute('aria-label') || '';
    let title = label;
    let subtitle = 'Помещение';
    let icon = '📍';

    if (label.startsWith('Помещение ')) {
      title = label.replace('Помещение ', '').trim();
      if (title.toLowerCase().includes('туалет') || title.toLowerCase().includes('wc')) {
        icon = '🚻';
        subtitle = 'Санитарный узел';
      } else if (title.toLowerCase().includes('столовая') || title.toLowerCase().includes('буфет') || title.toLowerCase().includes('кафе')) {
        icon = '🍽️';
        subtitle = 'Питание';
      } else if (title.toLowerCase().includes('медпункт')) {
        icon = '🏥';
        subtitle = 'Медицинский пункт';
      } else {
        subtitle = 'Аудитория';
      }
    } else if (label.toLowerCase().includes('лестница')) {
      icon = '🪜';
      title = label;
      subtitle = 'Перемещение между этажами';
    } else if (label.toLowerCase().includes('переход')) {
      icon = '🚶';
      title = label;
      subtitle = 'Переход между корпусами';
    } else if (label.toLowerCase().includes('лифт')) {
      icon = '🛗';
      title = label;
      subtitle = 'Лифт';
    }

    roomTitle.textContent = title;
    roomSubtitle.textContent = subtitle;
    roomIcon.textContent = icon;
    roomCard.classList.remove('hidden');
  }

  // Touch state
  let touchMode = 0;
  let panStartX = 0, panStartY = 0;
  let panStartTx = 0, panStartTy = 0;
  let touchMoved = false;

  let pinchStartDist = 0;
  let pinchStartScale = 1;
  let pinchStartMidX = 0, pinchStartMidY = 0;
  let pinchStartTx = 0, pinchStartTy = 0;

  function updateTransform(smooth) {
    container.style.transition = smooth ? 'transform 0.22s cubic-bezier(0.2, 0, 0, 1)' : 'none';
    container.style.transform = 'translate3d(' + translateX + 'px, ' + translateY + 'px, 0) scale(' + scale + ')';
  }

  function fitToScreen() {
    const svg = container.querySelector('svg');
    if (!svg) return;
    const vW = viewport.clientWidth;
    const vH = viewport.clientHeight;
    
    let sW = 3000, sH = 3000;
    const vb = svg.viewBox && svg.viewBox.baseVal;
    if (vb && vb.width > 0) {
      sW = vb.width;
      sH = vb.height;
    }
    
    const scaleX = (vW * 0.94) / sW;
    const scaleY = (vH * 0.94) / sH;
    scale = Math.min(scaleX, scaleY);
    if (scale <= 0) scale = 0.25;
    
    translateX = (vW - sW * scale) / 2;
    translateY = (vH - sH * scale) / 2;
    updateTransform(true);
  }

  window.zoomIn = function() {
    zoomAt(viewport.clientWidth / 2, viewport.clientHeight / 2, 1.4, true);
  };
  window.zoomOut = function() {
    zoomAt(viewport.clientWidth / 2, viewport.clientHeight / 2, 1 / 1.4, true);
  };
  window.resetView = function() {
    fitToScreen();
  };

  function zoomAt(clientX, clientY, factor, smooth) {
    const newScale = Math.max(0.06, Math.min(16.0, scale * factor));
    const factorApplied = newScale / scale;
    translateX = clientX - (clientX - translateX) * factorApplied;
    translateY = clientY - (clientY - translateY) * factorApplied;
    scale = newScale;
    updateTransform(Boolean(smooth));
  }

  // --- Touch event handling ---
  viewport.addEventListener('touchstart', (e) => {
    touchMoved = false;
    if (e.touches.length === 1) {
      touchMode = 1;
      panStartX = e.touches[0].clientX;
      panStartY = e.touches[0].clientY;
      panStartTx = translateX;
      panStartTy = translateY;
    } else if (e.touches.length >= 2) {
      touchMode = 2;
      touchMoved = true;
      const t0 = e.touches[0];
      const t1 = e.touches[1];
      pinchStartDist = Math.hypot(t0.clientX - t1.clientX, t0.clientY - t1.clientY);
      pinchStartScale = scale;
      pinchStartMidX = (t0.clientX + t1.clientX) / 2;
      pinchStartMidY = (t0.clientY + t1.clientY) / 2;
      pinchStartTx = translateX;
      pinchStartTy = translateY;
    }
  }, { passive: false });

  viewport.addEventListener('touchmove', (e) => {
    e.preventDefault();
    if (touchMode === 1 && e.touches.length === 1) {
      const dx = e.touches[0].clientX - panStartX;
      const dy = e.touches[0].clientY - panStartY;
      if (Math.hypot(dx, dy) > 8) {
        touchMoved = true;
      }
      translateX = panStartTx + dx;
      translateY = panStartTy + dy;
      updateTransform(false);
    } else if (touchMode === 2 && e.touches.length >= 2) {
      const t0 = e.touches[0];
      const t1 = e.touches[1];
      const curDist = Math.hypot(t0.clientX - t1.clientX, t0.clientY - t1.clientY);
      if (pinchStartDist > 0 && curDist > 0) {
        const curMidX = (t0.clientX + t1.clientX) / 2;
        const curMidY = (t0.clientY + t1.clientY) / 2;

        const ratio = curDist / pinchStartDist;
        const targetScale = Math.max(0.06, Math.min(16.0, pinchStartScale * ratio));
        const factor = targetScale / pinchStartScale;

        translateX = pinchStartMidX - (pinchStartMidX - pinchStartTx) * factor + (curMidX - pinchStartMidX);
        translateY = pinchStartMidY - (pinchStartMidY - pinchStartTy) * factor + (curMidY - pinchStartMidY);
        scale = targetScale;

        updateTransform(false);
      }
    }
  }, { passive: false });

  viewport.addEventListener('touchend', (e) => {
    if (e.touches.length === 1) {
      touchMode = 1;
      panStartX = e.touches[0].clientX;
      panStartY = e.touches[0].clientY;
      panStartTx = translateX;
      panStartTy = translateY;
    } else if (e.touches.length === 0) {
      if (!touchMoved) {
        const touch = e.changedTouches[0];
        const target = document.elementFromPoint(touch.clientX, touch.clientY);
        handleTapTarget(target);
      }
      touchMode = 0;
    }
  });

  viewport.addEventListener('touchcancel', () => {
    touchMode = 0;
  });

  function handleTapTarget(target) {
    if (!target) return;
    if (roomCard.contains(target)) return;
    const interactive = target.closest('g[role="button"], .Room');
    if (interactive && interactive.getAttribute('aria-label')) {
      selectRoom(interactive);
    } else {
      hideCard();
    }
  }

  // --- Mouse events for desktop/emulator ---
  let isMouseDown = false;
  let mouseStartX = 0, mouseStartY = 0;
  let mouseStartTx = 0, mouseStartTy = 0;
  let mouseMoved = false;

  viewport.addEventListener('mousedown', (e) => {
    if (roomCard.contains(e.target)) return;
    isMouseDown = true;
    mouseMoved = false;
    mouseStartX = e.clientX;
    mouseStartY = e.clientY;
    mouseStartTx = translateX;
    mouseStartTy = translateY;
  });

  window.addEventListener('mousemove', (e) => {
    if (!isMouseDown) return;
    const dx = e.clientX - mouseStartX;
    const dy = e.clientY - mouseStartY;
    if (Math.hypot(dx, dy) > 5) {
      mouseMoved = true;
    }
    translateX = mouseStartTx + dx;
    translateY = mouseStartTy + dy;
    updateTransform(false);
  });

  window.addEventListener('mouseup', (e) => {
    if (!isMouseDown) return;
    isMouseDown = false;
    if (!mouseMoved) {
      handleTapTarget(e.target);
    }
  });

  viewport.addEventListener('wheel', (e) => {
    e.preventDefault();
    const factor = e.deltaY < 0 ? 1.15 : 0.85;
    zoomAt(e.clientX, e.clientY, factor, false);
  }, { passive: false });

  // Double tap
  let lastTapTime = 0;
  viewport.addEventListener('touchend', (e) => {
    const now = Date.now();
    if (now - lastTapTime < 280) {
      e.preventDefault();
      if (scale > 1.2) {
        fitToScreen();
      } else {
        const touch = e.changedTouches[0];
        zoomAt(touch.clientX, touch.clientY, 2.2, true);
      }
    }
    lastTapTime = now;
  });

  // Initial fit
  window.addEventListener('DOMContentLoaded', () => {
    setTimeout(fitToScreen, 60);
  });
</script>
</body>
</html>
""".trimIndent()
    }
}
