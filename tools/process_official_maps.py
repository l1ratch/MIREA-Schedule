import os
import re
import shutil

SOURCE_DIR = r"E:\Projects Directory\MIREA-Schedule\maps"
TARGET_BASE = r"E:\Projects Directory\MIREA-Schedule\shared\src\commonMain\composeResources\files\maps"

CAMPUS_MAP = {
    "В-78": "v-78",
    "В-86": "v-86",
    "С-20": "s-20"
}

def polygon_centroid(pts):
    n = len(pts)
    if n < 3:
        xs = [p[0] for p in pts]
        ys = [p[1] for p in pts]
        return sum(xs)/len(xs), sum(ys)/len(ys)
    
    if pts[0] != pts[-1]:
        pts = pts + [pts[0]]
        n += 1
        
    area = 0.0
    cx = 0.0
    cy = 0.0
    for i in range(n - 1):
        x0, y0 = pts[i]
        x1, y1 = pts[i+1]
        cross = (x0 * y1 - x1 * y0)
        area += cross
        cx += (x0 + x1) * cross
        cy += (y0 + y1) * cross
    area *= 0.5
    if abs(area) < 1e-4:
        xs = [p[0] for p in pts]
        ys = [p[1] for p in pts]
        return sum(xs)/len(xs), sum(ys)/len(ys)
    cx /= (6.0 * area)
    cy /= (6.0 * area)
    return cx, cy

def clean_room_label(raw):
    name = raw.replace("Помещение ", "").strip()
    if name.startswith("Туалет "):
        suffix = name[7:].strip()
        return f"WC {suffix}"
    elif name == "Туалет":
        return "WC"
    if "," in name:
        parts = [p.strip() for p in name.split(",")]
        if len(parts) > 1 and len(name) > 16:
            return parts[0]
    return name

def extract_inner(svg_str):
    inner = re.sub(r'^\s*<svg[^>]*>', '', svg_str, flags=re.IGNORECASE)
    inner = re.sub(r'</svg>\s*$', '', inner, flags=re.IGNORECASE)
    return inner.strip()

def process_floor(geom_path, markers_path, out_svg_path):
    with open(geom_path, "r", encoding="utf-8") as f:
        geom_content = f.read().strip()
    with open(markers_path, "r", encoding="utf-8") as f:
        markers_content = f.read().strip()
        
    vb_match = re.search(r'viewBox="([^"]+)"', geom_content)
    if not vb_match:
        vb_match = re.search(r'viewBox="([^"]+)"', markers_content)
    viewbox = vb_match.group(1) if vb_match else "0 0 1000 1000"

    geom_inner = extract_inner(geom_content)
    markers_inner = extract_inner(markers_content)
    
    room_matches = re.finditer(r'<g\b[^>]*aria-label="([^"]+)"[^>]*>\s*<path\b[^>]*\bd="([^"]+)"[^>]*>', geom_inner)
    labels = []
    seen_labels = set()
    
    for m in room_matches:
        raw_label = m.group(1)
        d = m.group(2)
        label_text = clean_room_label(raw_label)
        if not label_text:
            continue
            
        coords = re.findall(r'([-+]?\d+(?:\.\d+)?)[,\s]+([-+]?\d+(?:\.\d+)?)', d)
        pts = [(float(x), float(y)) for x, y in coords]
        if len(pts) < 3:
            continue
            
        xs = [p[0] for p in pts]
        ys = [p[1] for p in pts]
        w = max(xs) - min(xs)
        h = max(ys) - min(ys)
        
        min_dim = min(w, h)
        if min_dim < 110:
            continue
            
        cx, cy = polygon_centroid(pts)
        
        if not (min(xs) <= cx <= max(xs) and min(ys) <= cy <= max(ys)):
            cx = (min(xs) + max(xs)) / 2.0
            cy = (min(ys) + max(ys)) / 2.0
            
        if min_dim >= 400:
            fs = 38
        elif min_dim >= 250:
            fs = 30
        elif min_dim >= 180:
            fs = 24
        else:
            fs = 20
            
        key = (round(cx, -1), round(cy, -1), label_text)
        if key in seen_labels:
            continue
        seen_labels.add(key)
        
        safe_text = label_text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace('"', "&quot;")
        labels.append(
            f'<text x="{cx:.1f}" y="{cy:.1f}" font-size="{fs}" class="room-label" text-anchor="middle" dominant-baseline="central">{safe_text}</text>'
        )

    labels_markup = "\n    ".join(labels)
    
    svg_out = f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="{viewbox}" width="100%" height="100%" preserveAspectRatio="xMidYMid meet">
  <g id="geometry">
    {geom_inner}
  </g>
  <g id="room-labels">
    {labels_markup}
  </g>
  <g id="markers">
    {markers_inner}
  </g>
</svg>"""

    os.makedirs(os.path.dirname(out_svg_path), exist_ok=True)
    with open(out_svg_path, "w", encoding="utf-8") as f:
        f.write(svg_out)
        
    return len(labels)

def main():
    for cid in ["v-78", "s-20", "v-86"]:
        target_dir = os.path.join(TARGET_BASE, cid)
        if os.path.exists(target_dir):
            print(f"Cleaning existing directory: {target_dir}")
            shutil.rmtree(target_dir)
        os.makedirs(target_dir, exist_ok=True)

    total_floors = 0
    for folder_name, cid in CAMPUS_MAP.items():
        campus_src = os.path.join(SOURCE_DIR, folder_name)
        if not os.path.isdir(campus_src):
            print(f"Directory not found: {campus_src}")
            continue
            
        for floor_dir in sorted(os.listdir(campus_src)):
            floor_path = os.path.join(campus_src, floor_dir)
            if not os.path.isdir(floor_path):
                continue
                
            m = re.search(r'(\d+)', floor_dir)
            if not m:
                continue
            floor_num = int(m.group(1))
            
            geom_file = None
            markers_file = None
            for fname in os.listdir(floor_path):
                if "Геометрия" in fname:
                    geom_file = os.path.join(floor_path, fname)
                elif "Маркеры" in fname:
                    markers_file = os.path.join(floor_path, fname)
                    
            if not geom_file or not markers_file:
                print(f"Missing geometry or markers in {floor_path}")
                continue
                
            out_file = os.path.join(TARGET_BASE, cid, f"floor_{floor_num}.svg")
            lbl_count = process_floor(geom_file, markers_file, out_file)
            size_kb = os.path.getsize(out_file) / 1024
            print(f"Processed {cid} / floor {floor_num} -> {out_file} ({size_kb:.1f} KB, {lbl_count} labels)")
            total_floors += 1

    print(f"Done! Successfully generated {total_floors} official floor maps.")

if __name__ == "__main__":
    main()
