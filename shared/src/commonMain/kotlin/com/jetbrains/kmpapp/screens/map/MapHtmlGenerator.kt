package com.jetbrains.kmpapp.screens.map

object MapHtmlGenerator {

    fun generateHtml(
        svgContent: String,
        isDark: Boolean = true
    ): String {
        val themeStyles = if (isDark) {
            """
            /* Dark theme overrides */
            .BigAreaPath { fill: #151b26 !important; stroke: #2d3748 !important; }
            rect[fill="#F8F8F8"] { fill: #151b26 !important; stroke: #2d3748 !important; }
            rect[fill="#fff"], rect[fill="#FFFFFF"] { fill: #1e2638 !important; stroke: #2d3748 !important; }
            path[fill="#262A34"] { fill: #e2e8f0 !important; }
            path[fill="#000"], path[fill="#000000"] { fill: #cbd5e1 !important; }
            .Room:hover rect, .Room:active rect { fill: #3b82f6 !important; }
            """
        } else {
            """
            /* Light theme styles */
            .BigAreaPath { fill: #f1f5f9 !important; stroke: #cbd5e1 !important; }
            rect[fill="#F8F8F8"] { fill: #f8fafc !important; stroke: #cbd5e1 !important; }
            rect[fill="#fff"], rect[fill="#FFFFFF"] { fill: #ffffff !important; stroke: #cbd5e1 !important; }
            path[fill="#262A34"] { fill: #1e293b !important; }
            path[fill="#000"], path[fill="#000000"] { fill: #0f172a !important; }
            .Room:hover rect, .Room:active rect { fill: #60a5fa !important; }
            """
        }

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
</style>
</head>
<body>
<div id="viewport">
  <div id="svg-container">
    $svgContent
  </div>
</div>
<script>
  const viewport = document.getElementById('viewport');
  const container = document.getElementById('svg-container');
  
  let scale = 1;
  let translateX = 0;
  let translateY = 0;

  // Touch state
  let touchMode = 0; // 0: idle, 1: pan, 2: pinch
  let panStartX = 0, panStartY = 0;
  let panStartTx = 0, panStartTy = 0;

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

  // --- Touch event handling (native mobile multi-touch) ---
  viewport.addEventListener('touchstart', (e) => {
    e.preventDefault();
    if (e.touches.length === 1) {
      touchMode = 1;
      panStartX = e.touches[0].clientX;
      panStartY = e.touches[0].clientY;
      panStartTx = translateX;
      panStartTy = translateY;
    } else if (e.touches.length >= 2) {
      touchMode = 2;
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
      touchMode = 0;
    }
  });

  viewport.addEventListener('touchcancel', () => {
    touchMode = 0;
  });

  // --- Mouse events for desktop/emulator ---
  let isMouseDown = false;
  let mouseStartX = 0, mouseStartY = 0;
  let mouseStartTx = 0, mouseStartTy = 0;

  viewport.addEventListener('mousedown', (e) => {
    isMouseDown = true;
    mouseStartX = e.clientX;
    mouseStartY = e.clientY;
    mouseStartTx = translateX;
    mouseStartTy = translateY;
  });

  window.addEventListener('mousemove', (e) => {
    if (!isMouseDown) return;
    translateX = mouseStartTx + (e.clientX - mouseStartX);
    translateY = mouseStartTy + (e.clientY - mouseStartY);
    updateTransform(false);
  });

  window.addEventListener('mouseup', () => {
    isMouseDown = false;
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
    setTimeout(fitToScreen, 50);
  });
</script>
</body>
</html>
""".trimIndent()
    }
}
