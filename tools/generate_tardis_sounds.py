"""
Synthétise les sons du TARDIS et du tournevis sonique (aucun enregistrement : tout est calculé).

- tardis/takeoff.ogg, tardis/landing.ogg : remplacés par les sons fournis par David
  (les fonctions takeoff() et landing() restent disponibles)
- tardis/landed.ogg  : choc sourd quand la cabine est posée
- tardis/enter.ogg   : passage de la cabine à la salle de console (souffle + petit « vworp »)
- tardis/hum.ogg     : bourdonnement de la salle de console (boucle parfaite de 4 s)
- item/sonic.ogg     : sifflement modulé du tournevis sonique

Python 3 pur + FFmpeg (avec libvorbis) pour convertir en .ogg.
Utilisation (depuis la racine du projet) :
    python tools/generate_tardis_sounds.py
"""
import array
import glob
import math
import os
import random
import subprocess
import tempfile
import wave

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "doctorwho", "sounds")
FFMPEG = (glob.glob(os.path.expandvars(r"%LOCALAPPDATA%\Microsoft\WinGet\Packages\Gyan.FFmpeg*\*\bin\ffmpeg.exe")) or ["ffmpeg"])[0]
RATE = 22050
TAU = 2 * math.pi


def to_ogg(samples, name):
    """Normalise (crête à 90 %), écrit un .wav temporaire puis le convertit en .ogg."""
    peak = max(1e-9, max(abs(s) for s in samples))
    data = array.array("h", (int(max(-1.0, min(1.0, s / peak * 0.9)) * 32767) for s in samples))
    out = os.path.join(ROOT, name + ".ogg")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    fd, tmp = tempfile.mkstemp(suffix=".wav")
    os.close(fd)
    with wave.open(tmp, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(data.tobytes())
    subprocess.run([FFMPEG, "-y", "-loglevel", "error", "-i", tmp, "-c:a", "libvorbis", "-q:a", "5", out], check=True)
    os.remove(tmp)
    print("OK", name, f"{len(samples) / RATE:.1f} s")


def echo(samples, delays=(0.11, 0.17, 0.29), feedback=0.33):
    """Petite réverbération : quelques échos qui se répètent (la cabine « résonne »)."""
    out = list(samples) + [0.0] * int(RATE * 1.2)
    for delay in delays:
        d = int(delay * RATE)
        for i in range(d, len(out)):
            out[i] += out[i - d] * feedback / len(delays)
    return out


def wheeze(cycle_count, cycle_length, envelope, seed):
    """
    Le « vworp » : à chaque cycle, un grincement métallique monte puis redescend.
    Partiels inharmoniques (son de métal frotté) + souffle filtré qui suit la hauteur.
    envelope(t) donne le volume global (0..1) selon le temps t en secondes.
    """
    rng = random.Random(seed)
    partials = [(1.0, 1.0), (2.01, 0.55), (2.98, 0.38), (4.23, 0.22), (5.41, 0.15), (6.93, 0.09)]
    phases = [0.0] * len(partials)
    n = int(cycle_count * cycle_length * RATE)
    out = [0.0] * n
    low = 0.0
    band = 0.0
    for i in range(n):
        t = i / RATE
        p = (t / cycle_length) % 1.0
        swell = math.sin(math.pi * p) ** 1.4
        freq = 62 + 120 * swell + 6 * math.sin(TAU * 5.5 * t)
        tone = 0.0
        for k, (ratio, amp) in enumerate(partials):
            phases[k] += TAU * freq * ratio / RATE
            tone += amp * math.sin(phases[k])
        # Souffle : bruit passé dans un filtre passe-bande grossier qui monte avec le grincement
        noise = rng.uniform(-1, 1)
        cutoff = 0.05 + 0.12 * swell
        low += cutoff * (noise - low)
        band += 0.5 * (low - band)
        scrape = (low - band) * 3.0
        out[i] = envelope(t) * (0.15 + 0.85 * swell) * (0.75 * tone + 0.6 * scrape)
    return out


def takeoff():
    cycles, length = 5, 1.35
    total = cycles * length

    def env(t):
        attack = min(1.0, t / 0.25)
        return attack * max(0.0, 1.0 - t / total) ** 0.8

    to_ogg(echo(wheeze(cycles, length, env, 1)), "tardis/takeoff")


def landing():
    cycles, length = 5, 1.35
    total = cycles * length

    def env(t):
        rise = min(1.0, (t / total) ** 0.6 * 1.2)
        release = min(1.0, (total - t) / 0.3)
        return max(0.0, rise * release)

    to_ogg(echo(wheeze(cycles, length, env, 2)), "tardis/landing")


def landed():
    """Choc sourd : basse fréquence qui retombe + petit claquement métallique."""
    rng = random.Random(3)
    n = int(1.6 * RATE)
    out = []
    phase = 0.0
    low = 0.0
    for i in range(n):
        t = i / RATE
        freq = 38 + 40 * math.exp(-t * 9)
        phase += TAU * freq / RATE
        boom = math.sin(phase) * math.exp(-t * 3.5)
        low += 0.08 * (rng.uniform(-1, 1) - low)
        clank = low * 2.5 * math.exp(-t * 18) + 0.25 * math.sin(TAU * 660 * t) * math.exp(-t * 12)
        out.append(boom + clank)
    to_ogg(echo(out, delays=(0.09, 0.15), feedback=0.25), "tardis/landed")


def enter():
    """Passage dans la salle de console : un souffle qui monte puis un court « vworp » qui s'éteint."""
    length = 1.0
    swoosh_rng = random.Random(4)
    low = 0.0
    swoosh = []
    for i in range(int(0.6 * RATE)):
        t = i / RATE
        low += (0.04 + 0.3 * t) * (swoosh_rng.uniform(-1, 1) - low)
        swoosh.append(low * 2.0 * math.sin(math.pi * t / 0.6))

    def env(t):
        return min(1.0, t / 0.1) * max(0.0, 1.0 - t / length) ** 1.5

    wheel = wheeze(1, length, env, 5)
    out = [0.0] * (len(swoosh) // 2 + len(wheel))
    for i, s in enumerate(swoosh):
        out[i] += s
    for i, s in enumerate(wheel):
        out[i + len(swoosh) // 2] += 0.8 * s
    to_ogg(echo(out), "tardis/enter")


def hum():
    """
    Bourdonnement de la salle de console. Toutes les fréquences font un nombre entier
    de cycles en 4 s : la boucle se raccorde sans clic.
    """
    length = 4.0
    n = int(length * RATE)
    voices = [(55.0, 1.0), (110.0, 0.5), (82.5, 0.35), (165.0, 0.22), (220.25, 0.08), (440.5, 0.03)]
    out = []
    for i in range(n):
        t = i / RATE
        swell = 0.8 + 0.2 * math.sin(TAU * 0.5 * t)       # 2 cycles par boucle
        shimmer = 0.5 + 0.5 * math.sin(TAU * 0.25 * t)     # 1 cycle par boucle
        s = 0.0
        for k, (f, a) in enumerate(voices):
            mod = shimmer if k >= 4 else swell
            s += a * mod * math.sin(TAU * f * t)
        out.append(s)
    to_ogg(out, "tardis/hum")


def sonic():
    """Sifflement aigu, avec un vibrato rapide (le « vwiiiiii » du tournevis)."""
    n = int(1.3 * RATE)
    out = []
    phase = 0.0
    for i in range(n):
        t = i / RATE
        attack = min(1.0, t / 0.06)
        release = min(1.0, (1.3 - t) / 0.2)
        freq = 2300 + 250 * min(1.0, t / 0.4) + 220 * math.sin(TAU * 17 * t)
        phase += TAU * freq / RATE
        s = math.sin(phase) + 0.3 * math.sin(2 * phase) + 0.12 * math.sin(3.01 * phase)
        tremolo = 0.85 + 0.15 * math.sin(TAU * 34 * t)
        out.append(attack * release * tremolo * s)
    to_ogg(out, "item/sonic")


if __name__ == "__main__":
    # takeoff.ogg et landing.ogg sont les sons fournis par David (baissés de ~10 dB, en mono) :
    # on ne les écrase pas. Pour revenir aux sons synthétisés, décommenter les deux lignes.
    # takeoff()
    # landing()
    landed()
    enter()
    hum()
    sonic()
