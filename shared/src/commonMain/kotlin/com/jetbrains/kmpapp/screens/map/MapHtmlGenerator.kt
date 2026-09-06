package com.jetbrains.kmpapp.screens.map

object MapHtmlGenerator {

    fun generateHtml(
        svgContent: String,
        isDark: Boolean = true
    ): String {
        // 1. Parse viewBox from SVG content to calculate concrete base canvas dimensions
        val vbRegex = Regex("""viewBox=["']\s*([-\d.]+)\s+([-\d.]+)\s+([-\d.]+)\s+([-\d.]+)\s*["']""")
        val vbMatch = vbRegex.find(svgContent)
        var baseW = 4000
        var baseH = 4000
        if (vbMatch != null) {
            val w = vbMatch.groupValues[3].toDoubleOrNull() ?: 4000.0
            val h = vbMatch.groupValues[4].toDoubleOrNull() ?: 4000.0
            val aspect = if (h > 0) w / h else 1.0
            val maxDim = 4000.0
            if (aspect >= 1.0) {
                baseW = maxDim.toInt()
                baseH = (maxDim / aspect).toInt().coerceAtLeast(600)
            } else {
                baseH = maxDim.toInt()
                baseW = (maxDim * aspect).toInt().coerceAtLeast(600)
            }
        }

        // 2. Prepare SVG root tag: ensure width and height are 100% inside our fixed-size container
        val svgTagRegex = Regex("""<svg\b([^>]*)>""")
        val firstSvgMatch = svgTagRegex.find(svgContent)
        val preparedSvg = if (firstSvgMatch != null) {
            var tag = firstSvgMatch.value
            tag = tag.replace(Regex("""\bwidth=["'][^"']*["']"""), "")
            tag = tag.replace(Regex("""\bheight=["'][^"']*["']"""), "")
            tag = tag.replace(Regex("""style=["'][^"']*["']"""), "")
            val newTag = tag.replace(">", """ width="100%" height="100%" preserveAspectRatio="xMidYMid meet" style="display:block;width:100%;height:100%;overflow:visible;">""")
            svgContent.substring(0, firstSvgMatch.range.first) + newTag + svgContent.substring(firstSvgMatch.range.last + 1)
        } else {
            svgContent
        }

        val themeStyles = if (isDark) {
            """
            /* Official pulse maps - Dark theme */
            g[role="button"] path {
              fill: #222b3d !important;
              stroke: #475569 !important;
              stroke-width: 1.5px !important;
              vector-effect: non-scaling-stroke !important;
              cursor: pointer;
              transition: fill 0.15s ease, stroke 0.15s ease;
            }
            g[style*="pointer-events: none"] > path {
              fill: #151b26 !important;
              stroke: #334155 !important;
              stroke-width: 1px !important;
              vector-effect: non-scaling-stroke !important;
            }
            .room-label {
              fill: #f1f5f9 !important;
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif !important;
              font-weight: 700 !important;
              letter-spacing: 0.5px;
              stroke: #0f141c !important;
              stroke-width: 2.5px !important;
              paint-order: stroke fill !important;
              pointer-events: none;
            }
            #markers text {
              fill: #60a5fa !important;
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif !important;
              stroke: #0f141c !important;
              stroke-width: 3.5px !important;
              paint-order: stroke fill !important;
              font-weight: 700 !important;
              pointer-events: none;
            }
            #markers rect {
              fill: #1e3a8a !important;
              stroke: #60a5fa !important;
              vector-effect: non-scaling-stroke !important;
            }
            #markers path {
              stroke: #60a5fa !important;
              vector-effect: non-scaling-stroke !important;
            }
            .selected-room > path {
              fill: #2563eb !important;
              stroke: #93c5fd !important;
              stroke-width: 3px !important;
              vector-effect: non-scaling-stroke !important;
            }

            /* Legacy MP-1 dark theme */
            .BigAreaPath { fill: #151b26 !important; stroke: #334155 !important; vector-effect: non-scaling-stroke !important; }
            rect[fill="#F8F8F8"] { fill: #151b26 !important; stroke: #334155 !important; vector-effect: non-scaling-stroke !important; }
            rect[fill="#fff"], rect[fill="#FFFFFF"] { fill: #222b3d !important; stroke: #475569 !important; vector-effect: non-scaling-stroke !important; }
            path[fill="#262A34"] { fill: #e6edf3 !important; }
            path[fill="#000"], path[fill="#000000"] { fill: #cbd5e1 !important; }
            .Room:hover rect, .Room:active rect { fill: #2563eb !important; }
            """
        } else {
            """
            /* Official pulse maps - Light theme */
            g[role="button"] path {
              fill: #ffffff !important;
              stroke: #94a3b8 !important;
              stroke-width: 1.5px !important;
              vector-effect: non-scaling-stroke !important;
              cursor: pointer;
              transition: fill 0.15s ease, stroke 0.15s ease;
            }
            g[style*="pointer-events: none"] > path {
              fill: #e2e8f0 !important;
              stroke: #cbd5e1 !important;
              stroke-width: 1px !important;
              vector-effect: non-scaling-stroke !important;
            }
            .room-label {
              fill: #0f172a !important;
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif !important;
              font-weight: 700 !important;
              letter-spacing: 0.5px;
              stroke: #ffffff !important;
              stroke-width: 2.5px !important;
              paint-order: stroke fill !important;
              pointer-events: none;
            }
            #markers text {
              fill: #2563eb !important;
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif !important;
              stroke: #ffffff !important;
              stroke-width: 3.5px !important;
              paint-order: stroke fill !important;
              font-weight: 700 !important;
              pointer-events: none;
            }
            #markers rect {
              fill: #eff6ff !important;
              stroke: #2563eb !important;
              vector-effect: non-scaling-stroke !important;
            }
            #markers path {
              stroke: #2563eb !important;
              vector-effect: non-scaling-stroke !important;
            }
            .selected-room > path {
              fill: #bfdbfe !important;
              stroke: #2563eb !important;
              stroke-width: 3px !important;
              vector-effect: non-scaling-stroke !important;
            }

            /* Legacy MP-1 light theme */
            .BigAreaPath { fill: #e2e8f0 !important; stroke: #cbd5e1 !important; vector-effect: non-scaling-stroke !important; }
            rect[fill="#F8F8F8"] { fill: #f1f5f9 !important; stroke: #cbd5e1 !important; vector-effect: non-scaling-stroke !important; }
            rect[fill="#fff"], rect[fill="#FFFFFF"] { fill: #ffffff !important; stroke: #94a3b8 !important; vector-effect: non-scaling-stroke !important; }
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
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=25.0, user-scalable=no">
<style>
  * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
  html, body {
    margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden;
    background-color: ${if (isDark) "#0b0f17" else "#f8fafc"}; user-select: none; -webkit-user-select: none;
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
    width: ${baseW}px;
    height: ${baseH}px;
  }
  #svg-container svg {
    display: block !important;
    width: 100% !important;
    height: 100% !important;
    max-width: none !important;
    overflow: visible !important;
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
    $preparedSvg
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

  let rafId = null;
  function updateTransform(smooth) {
    if (smooth) {
      if (rafId) { cancelAnimationFrame(rafId); rafId = null; }
      container.style.transition = 'transform 0.22s cubic-bezier(0.2, 0, 0, 1)';
      container.style.transform = 'translate3d(' + translateX + 'px, ' + translateY + 'px, 0) scale(' + scale + ')';
      return;
    }
    if (rafId) return;
    rafId = requestAnimationFrame(() => {
      rafId = null;
      container.style.transition = 'none';
      container.style.transform = 'translate3d(' + translateX + 'px, ' + translateY + 'px, 0) scale(' + scale + ')';
    });
  }

  function getContainerSize() {
    const w = container.offsetWidth || ${baseW};
    const h = container.offsetHeight || ${baseH};
    return { w: w > 0 ? w : ${baseW}, h: h > 0 ? h : ${baseH} };
  }

  function fitToScreen() {
    const vW = viewport.clientWidth || window.innerWidth;
    const vH = viewport.clientHeight || window.innerHeight;
    if (!vW || !vH) return;
    
    const size = getContainerSize();
    const sW = size.w;
    const sH = size.h;
    
    const scaleX = (vW * 0.94) / sW;
    const scaleY = (vH * 0.94) / sH;
    scale = Math.min(scaleX, scaleY);
    if (scale <= 0) scale = 0.2;
    
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
    const newScale = Math.max(0.01, Math.min(25.0, scale * factor));
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
        const targetScale = Math.max(0.01, Math.min(25.0, pinchStartScale * ratio));
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
      const fitSize = getContainerSize();
      const vW = viewport.clientWidth || window.innerWidth;
      const baseFitScale = (vW * 0.94) / fitSize.w;
      if (scale > baseFitScale * 1.5) {
        fitToScreen();
      } else {
        const touch = e.changedTouches[0];
        zoomAt(touch.clientX, touch.clientY, 2.5, true);
      }
    }
    lastTapTime = now;
  });

  // Multi-pass initial fit for reliable WebView geometry resolution
  function init() {
    fitToScreen();
    setTimeout(fitToScreen, 50);
    setTimeout(fitToScreen, 150);
    setTimeout(fitToScreen, 400);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
  window.addEventListener('load', init);
  window.addEventListener('resize', fitToScreen);
</script>
</body>
</html>
""".trimIndent()
    }
}
