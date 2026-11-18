"""
Packs GloVe word vectors (Stanford NLP, Public Domain Dedication and License) into the small files the
template search reads: app/src/main/assets/search/vocab.txt and vectors.bin.

    python3 scripts/pack_word_vectors.py path/to/glove.6B.100d.txt [extra words file]

Keeps the most frequent words (GloVe lists them most frequent first), plus any extra words the search's
concepts use. Each vector is normalised to length 1 and stored as int8 with one float scale:
vectors.bin = int32 count, int32 dim, then per word: float32 scale, dim x int8.
"""
import re
import struct
import sys
from pathlib import Path

KEEP = 60000
WORD = re.compile(r"^[a-z][a-z0-9'-]*$|^[0-9]+[a-z]+$")


def main():
    src = Path(sys.argv[1])
    extra = set()
    if len(sys.argv) > 2:
        extra = {w.strip() for w in Path(sys.argv[2]).read_text().split() if w.strip()}
    out = Path(__file__).resolve().parent.parent / "app/src/main/assets/search"
    out.mkdir(parents=True, exist_ok=True)

    words, vecs = [], []
    seen = set()
    with src.open(encoding="utf-8") as f:
        for line in f:
            parts = line.rstrip().split(" ")
            w = parts[0]
            if w in seen or not WORD.match(w) or len(w) > 24:
                continue
            if len(words) >= KEEP and w not in extra:
                continue
            v = [float(x) for x in parts[1:]]
            n = sum(x * x for x in v) ** 0.5 or 1.0
            v = [x / n for x in v]
            words.append(w)
            vecs.append(v)
            seen.add(w)
    dim = len(vecs[0])
    with (out / "vectors.bin").open("wb") as f:
        f.write(struct.pack("<ii", len(words), dim))
        for v in vecs:
            m = max(abs(x) for x in v) or 1.0
            scale = m / 127.0
            f.write(struct.pack("<f", scale))
            f.write(bytes((round(x / scale) & 0xFF) for x in v))
    (out / "vocab.txt").write_text("\n".join(words) + "\n", encoding="utf-8")
    missing = sorted(extra - seen)
    print(f"{len(words)} words x {dim}; missing extras: {missing}")


if __name__ == "__main__":
    main()
