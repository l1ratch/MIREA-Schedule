package com.jetbrains.kmpapp.screens.map

object MapHtmlGenerator {

    fun generateHtml(
        svgContent: String,
        isDark: Boolean = true
    ): String {
        // 1. Parse viewBox from SVG content to extract base coordinate space
        val vbRegex = Regex("""viewBox=["']\s*([-\d.]+)\s+([-\d.]+)\s+([-\d.]+)\s+([-\d.]+)\s*["']""")
        val vbMatch = vbRegex.find(svgContent)
        val origX = vbMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        val origY = vbMatch?.groupValues?.get(2)?.toDoubleOrNull() ?: 0.0
        val origW = vbMatch?.groupValues?.get(3)?.toDoubleOrNull() ?: 4000.0
        val origH = vbMatch?.groupValues?.get(4)?.toDoubleOrNull() ?: 4000.0

        // Note: We intentionally preserve the author's tailored font-size (20, 24, 30, 38)
        // on each SVG text element so labels fit cleanly inside their room geometry without collisions.

        // 2. Prepare SVG root tag: inject id="map-svg", width/height 100%, preserveAspectRatio
        val svgTagRegex = Regex("""<svg\b([^>]*)>""")
        val firstSvgMatch = svgTagRegex.find(svgContent)
        val preparedSvg = if (firstSvgMatch != null) {
            var tag = firstSvgMatch.value
            tag = tag.replace(Regex("""\bwidth=["'][^"']*["']"""), "")
            tag = tag.replace(Regex("""\bheight=["'][^"']*["']"""), "")
            tag = tag.replace(Regex("""\bid=["'][^"']*["']"""), "")
            tag = tag.replace(Regex("""\bpreserveAspectRatio=["'][^"']*["']"""), "")
            tag = tag.replace(Regex("""style=["'][^"']*["']"""), "")
            val newTag = tag.replace(">", """ id="map-svg" width="100%" height="100%" preserveAspectRatio="xMidYMid meet" style="display:block;width:100%;height:100%;touch-action:none;overflow:visible;">""")
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
              letter-spacing: 0.25px;
              stroke: #0f141c !important;
              stroke-width: 3px !important;
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
              letter-spacing: 0.25px;
              stroke: #ffffff !important;
              stroke-width: 3px !important;
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
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
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
  #map-svg {
    display: block !important;
    width: 100% !important;
    height: 100% !important;
    max-width: none !important;
    overflow: visible !important;
    touch-action: none;
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
  $preparedSvg
</div>
<script>
  const viewport = document.getElementById('viewport');
  const svg = document.getElementById('map-svg');
  const roomCard = document.getElementById('room-card');
  const roomTitle = document.getElementById('room-card-title');
  const roomSubtitle = document.getElementById('room-card-subtitle');
  const roomIcon = document.getElementById('room-card-icon');
  const roomClose = document.getElementById('room-card-close');

  const origX = $origX;
  const origY = $origY;
  const origW = $origW;
  const origH = $origH;
  const campusAspect = (origH > 0) ? (origW / origH) : 1.0;

  let curVx = origX;
  let curVy = origY;
  let curVw = origW;
  let curVh = origH;

  let initialVx = origX;
  let initialVy = origY;
  let initialVw = origW;
  let initialVh = origH;

  let minVw = 200;
  let maxVw = origW * 2.0;

  function applyViewBox() {
    svg.setAttribute('viewBox', curVx.toFixed(2) + ' ' + curVy.toFixed(2) + ' ' + curVw.toFixed(2) + ' ' + curVh.toFixed(2));
  }

  function calcInitialView() {
    const vW = viewport.clientWidth || window.innerWidth;
    const vH = viewport.clientHeight || window.innerHeight;
    if (!vW || !vH) return;
    const screenAspect = vW / vH;

    let targetVw, targetVh;
    if (screenAspect < 1.0) {
      // Portrait mobile orientation
      if (campusAspect > 1.8) {
        // Very wide campus (e.g. V-78): building fills ~68% of screen height
        targetVh = origH * 1.48;
        targetVw = targetVh * screenAspect;
      } else if (campusAspect >= 1.2) {
        // Moderately wide campus (e.g. V-86): building fills ~80% of screen height
        targetVh = origH * 1.25;
        targetVw = targetVh * screenAspect;
      } else {
        // Compact or tall campus (e.g. S-20, MP-1): fit campus with 10% padding
        const scaleX = origW / (vW * 0.90);
        const scaleY = origH / (vH * 0.90);
        const scale = Math.max(scaleX, scaleY);
        targetVw = vW * scale;
        targetVh = vH * scale;
      }
    } else {
      // Landscape or tablet orientation: fit campus with 10% padding
      const scaleX = origW / (vW * 0.92);
      const scaleY = origH / (vH * 0.92);
      const scale = Math.max(scaleX, scaleY);
      targetVw = vW * scale;
      targetVh = vH * scale;
    }

    const cx = origX + origW / 2;
    const cy = origY + origH / 2;
    initialVw = targetVw;
    initialVh = targetVh;
    initialVx = cx - targetVw / 2;
    initialVy = cy - targetVh / 2;

    minVw = Math.max(150, Math.min(400, origW * 0.05));
    maxVw = Math.max(origW * 1.6, origH * screenAspect * 1.6);
  }

  let animRafId = null;
  function animateTo(targetVx, targetVy, targetVw, targetVh, duration) {
    if (animRafId) { cancelAnimationFrame(animRafId); animRafId = null; }
    const startVx = curVx, startVy = curVy, startVw = curVw, startVh = curVh;
    const startTime = performance.now();
    const dur = duration || 240;

    function step(time) {
      const elapsed = time - startTime;
      const p = Math.min(1, elapsed / dur);
      // easeOutCubic curve
      const ease = 1 - Math.pow(1 - p, 3);
      curVx = startVx + (targetVx - startVx) * ease;
      curVy = startVy + (targetVy - startVy) * ease;
      curVw = startVw + (targetVw - startVw) * ease;
      curVh = startVh + (targetVh - startVh) * ease;
      applyViewBox();
      if (p < 1) {
        animRafId = requestAnimationFrame(step);
      } else {
        animRafId = null;
      }
    }
    animRafId = requestAnimationFrame(step);
  }

  function fitToScreen(smooth) {
    calcInitialView();
    if (smooth) {
      animateTo(initialVx, initialVy, initialVw, initialVh, 260);
    } else {
      curVx = initialVx;
      curVy = initialVy;
      curVw = initialVw;
      curVh = initialVh;
      applyViewBox();
    }
  }

  function zoomAt(clientX, clientY, factor, smooth) {
    const vW = viewport.clientWidth || window.innerWidth;
    const vH = viewport.clientHeight || window.innerHeight;
    if (!vW || !vH) return;

    const targetVw = Math.max(minVw, Math.min(maxVw, curVw / factor));
    const targetVh = targetVw * (vH / vW);
    const fracX = clientX / vW;
    const fracY = clientY / vH;

    const targetVx = curVx + fracX * (curVw - targetVw);
    const targetVy = curVy + fracY * (curVh - targetVh);

    if (smooth) {
      animateTo(targetVx, targetVy, targetVw, targetVh, 220);
    } else {
      curVx = targetVx;
      curVy = targetVy;
      curVw = targetVw;
      curVh = targetVh;
      applyViewBox();
    }
  }

  window.zoomIn = function() {
    const vW = viewport.clientWidth || window.innerWidth;
    const vH = viewport.clientHeight || window.innerHeight;
    zoomAt(vW / 2, vH / 2, 1.4, true);
  };
  window.zoomOut = function() {
    const vW = viewport.clientWidth || window.innerWidth;
    const vH = viewport.clientHeight || window.innerHeight;
    zoomAt(vW / 2, vH / 2, 1 / 1.4, true);
  };
  window.resetView = function() {
    fitToScreen(true);
  };

  // --- Room selection and Info Card ---
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

  // --- Touch event handling (Pan & Pinch Zoom) ---
  let touchMode = 0; // 0: idle, 1: pan, 2: pinch
  let touchMoved = false;
  let isPinching = false;

  let panStartX = 0, panStartY = 0;
  let panStartVx = 0, panStartVy = 0;

  let pinchStartDist = 0;
  let pinchStartVw = 0, pinchStartVh = 0;
  let pinchStartVx = 0, pinchStartVy = 0;
  let pinchStartMidX = 0, pinchStartMidY = 0;

  viewport.addEventListener('touchstart', (e) => {
    if (roomCard.contains(e.target)) return;
    if (animRafId) { cancelAnimationFrame(animRafId); animRafId = null; }
    touchMoved = false;

    if (e.touches.length === 1) {
      touchMode = 1;
      panStartX = e.touches[0].clientX;
      panStartY = e.touches[0].clientY;
      panStartVx = curVx;
      panStartVy = curVy;
    } else if (e.touches.length >= 2) {
      touchMode = 2;
      isPinching = true;
      touchMoved = true;
      const t0 = e.touches[0];
      const t1 = e.touches[1];
      pinchStartDist = Math.hypot(t0.clientX - t1.clientX, t0.clientY - t1.clientY);
      pinchStartVw = curVw;
      pinchStartVh = curVh;
      pinchStartVx = curVx;
      pinchStartVy = curVy;
      pinchStartMidX = (t0.clientX + t1.clientX) / 2;
      pinchStartMidY = (t0.clientY + t1.clientY) / 2;
    }
  }, { passive: false });

  viewport.addEventListener('touchmove', (e) => {
    if (roomCard.contains(e.target)) return;
    e.preventDefault();
    const vW = viewport.clientWidth || window.innerWidth;
    const vH = viewport.clientHeight || window.innerHeight;
    if (!vW || !vH) return;

    if (touchMode === 1 && e.touches.length === 1) {
      const dx = e.touches[0].clientX - panStartX;
      const dy = e.touches[0].clientY - panStartY;
      if (Math.hypot(dx, dy) > 6) {
        touchMoved = true;
      }
      const svgDx = dx * (curVw / vW);
      const svgDy = dy * (curVh / vH);
      curVx = panStartVx - svgDx;
      curVy = panStartVy - svgDy;
      applyViewBox();
    } else if (touchMode === 2 && e.touches.length >= 2) {
      touchMoved = true;
      const t0 = e.touches[0];
      const t1 = e.touches[1];
      const curDist = Math.hypot(t0.clientX - t1.clientX, t0.clientY - t1.clientY);
      if (pinchStartDist > 0 && curDist > 0) {
        const curMidX = (t0.clientX + t1.clientX) / 2;
        const curMidY = (t0.clientY + t1.clientY) / 2;

        const scaleFactor = curDist / pinchStartDist;
        let targetVw = pinchStartVw / scaleFactor;
        targetVw = Math.max(minVw, Math.min(maxVw, targetVw));
        const targetVh = targetVw * (vH / vW);

        const fracX = pinchStartMidX / vW;
        const fracY = pinchStartMidY / vH;

        const midDx = (curMidX - pinchStartMidX) * (targetVw / vW);
        const midDy = (curMidY - pinchStartMidY) * (targetVh / vH);

        curVx = pinchStartVx + fracX * (pinchStartVw - targetVw) - midDx;
        curVy = pinchStartVy + fracY * (pinchStartVh - targetVh) - midDy;
        curVw = targetVw;
        curVh = targetVh;
        applyViewBox();
      }
    }
  }, { passive: false });

  let lastTapTime = 0;
  let lastTapX = 0, lastTapY = 0;

  viewport.addEventListener('touchend', (e) => {
    if (e.touches.length === 1) {
      // Transition from 2 fingers to 1 finger smoothly without jumping
      touchMode = 1;
      panStartX = e.touches[0].clientX;
      panStartY = e.touches[0].clientY;
      panStartVx = curVx;
      panStartVy = curVy;
      touchMoved = true;
    } else if (e.touches.length === 0) {
      if (!touchMoved && !isPinching) {
        const touch = e.changedTouches[0];
        const now = Date.now();
        const isDoubleTap = (now - lastTapTime < 300) && (Math.hypot(touch.clientX - lastTapX, touch.clientY - lastTapY) < 40);
        lastTapTime = now;
        lastTapX = touch.clientX;
        lastTapY = touch.clientY;

        if (isDoubleTap) {
          if (curVw < initialVw * 0.75) {
            fitToScreen(true);
          } else {
            zoomAt(touch.clientX, touch.clientY, 2.5, true);
          }
        } else {
          const target = document.elementFromPoint(touch.clientX, touch.clientY);
          handleTapTarget(target);
        }
      }
      touchMode = 0;
      setTimeout(() => { isPinching = false; }, 80);
    }
  });

  viewport.addEventListener('touchcancel', () => {
    touchMode = 0;
    isPinching = false;
  });

  // --- Mouse events for desktop/emulator preview ---
  let isMouseDown = false;
  let mouseStartX = 0, mouseStartY = 0;
  let mouseStartVx = 0, mouseStartVy = 0;
  let mouseMoved = false;

  viewport.addEventListener('mousedown', (e) => {
    if (roomCard.contains(e.target)) return;
    if (animRafId) { cancelAnimationFrame(animRafId); animRafId = null; }
    isMouseDown = true;
    mouseMoved = false;
    mouseStartX = e.clientX;
    mouseStartY = e.clientY;
    mouseStartVx = curVx;
    mouseStartVy = curVy;
  });

  window.addEventListener('mousemove', (e) => {
    if (!isMouseDown) return;
    const dx = e.clientX - mouseStartX;
    const dy = e.clientY - mouseStartY;
    if (Math.hypot(dx, dy) > 5) {
      mouseMoved = true;
    }
    const vW = viewport.clientWidth || window.innerWidth;
    const vH = viewport.clientHeight || window.innerHeight;
    const svgDx = dx * (curVw / vW);
    const svgDy = dy * (curVh / vH);
    curVx = mouseStartVx - svgDx;
    curVy = mouseStartVy - svgDy;
    applyViewBox();
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
    const factor = e.deltaY < 0 ? 1.2 : 0.833;
    zoomAt(e.clientX, e.clientY, factor, false);
  }, { passive: false });

  // --- Responsive Initialization ---
  function init() {
    calcInitialView();
    curVx = initialVx;
    curVy = initialVy;
    curVw = initialVw;
    curVh = initialVh;
    applyViewBox();
  }

  function multiPassInit() {
    init();
    setTimeout(init, 50);
    setTimeout(init, 150);
    setTimeout(init, 400);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', multiPassInit);
  } else {
    multiPassInit();
  }
  window.addEventListener('load', multiPassInit);
  window.addEventListener('resize', () => {
    fitToScreen(false);
  });
</script>
</body>
</html>
""".trimIndent()
    }
}
