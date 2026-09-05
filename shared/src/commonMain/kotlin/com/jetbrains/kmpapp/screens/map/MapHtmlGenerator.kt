package com.jetbrains.kmpapp.screens.map

object MapHtmlGenerator {

    fun generateHtml(
        svgContent: String,
        highlightRoom: String? = null
    ): String {
        val safeSvg = svgContent
            .replace("\"", "\\\"")
            .replace("\n", " ")
            .replace("\r", " ")

        return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=no">
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
  }
  #viewport:active { cursor: grabbing; }
  #svg-container {
    position: absolute;
    transform-origin: 0 0;
    will-change: transform;
  }
  svg {
    display: block;
    width: auto;
    height: auto;
    max-width: none;
  }
  /* Dark theme overrides */
  .BigAreaPath { fill: #151b26 !important; stroke: #2d3748 !important; }
  rect[fill="#F8F8F8"] { fill: #151b26 !important; stroke: #2d3748 !important; }
  rect[fill="#fff"], rect[fill="#FFFFFF"] { fill: #1e2638 !important; stroke: #2d3748 !important; }
  path[fill="#262A34"] { fill: #e2e8f0 !important; }
  path[fill="#000"], path[fill="#000000"] { fill: #cbd5e1 !important; }
  
  .Room rect, .Room path {
    cursor: pointer;
    transition: fill 0.15s ease, stroke 0.15s ease;
  }
  .Room:hover rect, .Room:active rect {
    fill: #3b82f6 !important;
  }
  .room-highlight rect {
    fill: #2563eb !important;
    stroke: #60a5fa !important;
    stroke-width: 4px !important;
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
  
  let pointers = new Map();
  let prevDiff = -1;
  let prevMidX = 0, prevMidY = 0;
  let isPanning = false;
  let startX = 0, startY = 0;
  let originX = 0, originY = 0;

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
    const newScale = Math.max(0.08, Math.min(12, scale * factor));
    const factorApplied = newScale / scale;
    translateX = clientX - (clientX - translateX) * factorApplied;
    translateY = clientY - (clientY - translateY) * factorApplied;
    scale = newScale;
    updateTransform(Boolean(smooth));
  }

  // Pointer events
  viewport.addEventListener('pointerdown', (e) => {
    e.preventDefault();
    pointers.set(e.pointerId, e);
    if (pointers.size === 1) {
      isPanning = true;
      startX = e.clientX;
      startY = e.clientY;
      originX = translateX;
      originY = translateY;
    } else if (pointers.size === 2) {
      isPanning = false;
      const pts = Array.from(pointers.values());
      prevDiff = Math.hypot(pts[0].clientX - pts[1].clientX, pts[0].clientY - pts[1].clientY);
      prevMidX = (pts[0].clientX + pts[1].clientX) / 2;
      prevMidY = (pts[0].clientY + pts[1].clientY) / 2;
    }
  });

  viewport.addEventListener('pointermove', (e) => {
    if (!pointers.has(e.pointerId)) return;
    e.preventDefault();
    pointers.set(e.pointerId, e);

    if (pointers.size === 1 && isPanning) {
      const dx = e.clientX - startX;
      const dy = e.clientY - startY;
      translateX = originX + dx;
      translateY = originY + dy;
      updateTransform(false);
    } else if (pointers.size === 2) {
      isPanning = false;
      const pts = Array.from(pointers.values());
      const curDiff = Math.hypot(pts[0].clientX - pts[1].clientX, pts[0].clientY - pts[1].clientY);
      const midX = (pts[0].clientX + pts[1].clientX) / 2;
      const midY = (pts[0].clientY + pts[1].clientY) / 2;

      if (prevDiff > 0 && curDiff > 0) {
        const factor = curDiff / prevDiff;
        // Clamp factor per movement to avoid abrupt jumps
        const clampedFactor = Math.max(0.6, Math.min(1.8, factor));
        const newScale = Math.max(0.08, Math.min(12, scale * clampedFactor));
        const factorApplied = newScale / scale;
        
        // Scale around the midpoint
        translateX = midX - (midX - translateX) * factorApplied;
        translateY = midY - (midY - translateY) * factorApplied;
        scale = newScale;

        // Pan with the midpoint movement
        translateX += (midX - prevMidX);
        translateY += (midY - prevMidY);

        updateTransform(false);
      }
      prevDiff = curDiff;
      prevMidX = midX;
      prevMidY = midY;
    }
  });

  function endPointer(e) {
    pointers.delete(e.pointerId);
    if (pointers.size < 2) {
      prevDiff = -1;
    }
    if (pointers.size === 1) {
      // Seamless handover to 1-finger pan without jumping
      const remaining = Array.from(pointers.values())[0];
      startX = remaining.clientX;
      startY = remaining.clientY;
      originX = translateX;
      originY = translateY;
      isPanning = true;
    } else if (pointers.size === 0) {
      isPanning = false;
    }
  }

  viewport.addEventListener('pointerup', endPointer);
  viewport.addEventListener('pointercancel', endPointer);

  viewport.addEventListener('wheel', (e) => {
    e.preventDefault();
    const factor = e.deltaY < 0 ? 1.15 : 0.85;
    zoomAt(e.clientX, e.clientY, factor, false);
  }, { passive: false });

  // Double tap
  let lastTap = 0;
  viewport.addEventListener('touchend', (e) => {
    const now = Date.now();
    if (now - lastTap < 300) {
      e.preventDefault();
      if (scale > 1.0) {
        fitToScreen();
      } else {
        const touch = e.changedTouches[0];
        zoomAt(touch.clientX, touch.clientY, 2.2, true);
      }
    }
    lastTap = now;
  });

  // Touch move passive prevention
  document.addEventListener('touchmove', (e) => {
    if (pointers.size > 0) {
      e.preventDefault();
    }
  }, { passive: false });

  window.addEventListener('DOMContentLoaded', () => {
    setTimeout(fitToScreen, 60);
  });
  window.addEventListener('resize', fitToScreen);
</script>
</body>
</html>
""".trimIndent()
    }
}
