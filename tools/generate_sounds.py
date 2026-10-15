"""
Génère les voix et bruitages des créatures (Windows uniquement).

1. La voix française de Windows (System.Speech, « Hortense ») dit la réplique -> .wav
2. Effet en Python : modulation en anneau (voix de Dalek), voix métallique (Cyberman)...
3. FFmpeg convertit en .ogg (format des sons Minecraft).

Utilisation (depuis la racine du projet) :
    python tools/generate_sounds.py
"""
import array
import glob
import math
import os
import subprocess
import tempfile
import wave

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "doctorwho", "sounds", "entity")
FFMPEG = (glob.glob(os.path.expandvars(r"%LOCALAPPDATA%\Microsoft\WinGet\Packages\Gyan.FFmpeg*\*\bin\ffmpeg.exe")) or ["ffmpeg"])[0]
RATE = 22050


def speak(text, path, rate=0, voice="Microsoft Hortense Desktop"):
    script = (
        "Add-Type -AssemblyName System.Speech;"
        "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer;"
        f"try {{ $s.SelectVoice('{voice}') }} catch {{}};"
        f"$s.Rate = {rate};"
        f"$f = New-Object System.Speech.AudioFormat.SpeechAudioFormatInfo({RATE}, 16, 1);"
        f"$s.SetOutputToWaveFile('{path}', $f);"
        f"$s.Speak('{text}'); $s.Dispose()"
    )
    subprocess.run(["powershell", "-NoProfile", "-Command", script], check=True)


def read(path):
    with wave.open(path, "rb") as w:
        return array.array("h", w.readframes(w.getnframes()))


def to_ogg(samples, name):
    os.makedirs(ROOT, exist_ok=True)
    tmp = os.path.join(tempfile.gettempdir(), "dw_" + name + ".wav")
    with wave.open(tmp, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(samples.tobytes())
    out = os.path.join(ROOT, name + ".ogg")
    subprocess.run([FFMPEG, "-y", "-loglevel", "error", "-i", tmp, "-c:a", "libvorbis", "-q:a", "5", out], check=True)
    print("OK son", name)


def clip(v):
    return max(-32767, min(32767, int(v)))


def dalek_voice(samples):
    """Modulation en anneau à 30 Hz + saturation : la voix grinçante des Daleks."""
    out = array.array("h")
    for i, v in enumerate(samples):
        m = math.sin(2 * math.pi * 30 * i / RATE)
        x = v * m * 1.8
        out.append(clip(32767 * math.tanh(x / 20000)))
    return out


def cyber_voice(samples):
    """Voix métallique : mélange avec un écho très court + modulation légère."""
    out = array.array("h")
    d = int(RATE * 0.011)
    for i, v in enumerate(samples):
        echo = samples[i - d] if i >= d else 0
        m = 0.6 + 0.4 * math.sin(2 * math.pi * 55 * i / RATE)
        out.append(clip((v + 0.7 * echo) * m))
    return out


def ood_voice(samples):
    """Voix douce et lointaine : plus grave (rééchantillonnée) avec un écho."""
    slow = array.array("h", (samples[int(i * 0.85)] for i in range(int(len(samples) / 0.85))))
    out = array.array("h")
    d = int(RATE * 0.05)
    for i, v in enumerate(slow):
        echo = slow[i - d] if i >= d else 0
        out.append(clip(0.85 * v + 0.2 * echo))
    return out


def laser():
    """Rayon de Dalek : sifflement descendant + grésillement."""
    n = int(RATE * 0.6)
    out = array.array("h")
    phase = 0.0
    for i in range(n):
        t = i / n
        freq = 1800 * (1 - t) + 300
        phase += 2 * math.pi * freq / RATE
        v = math.sin(phase) * 0.6 + math.sin(phase * 3.01) * 0.25
        v *= (1 - t) ** 1.5
        out.append(clip(v * 26000))
    return out


def voice(text, name, effect, rate=0):
    tmp = os.path.join(tempfile.gettempdir(), "dw_tts_" + name + ".wav")
    speak(text, tmp, rate)
    to_ogg(effect(read(tmp)), name)


if __name__ == "__main__":
    voice("Exterminer !", "dalek_exterminate", dalek_voice, rate=1)
    voice("Extermination !", "dalek_extermination", dalek_voice, rate=1)
    voice("Je suis un Dalek.", "dalek_ambient", dalek_voice)
    to_ogg(laser(), "dalek_laser")
    voice("Supprimer !", "cyberman_delete", cyber_voice, rate=-1)
    voice("Vous serez améliorés.", "cyberman_ambient", cyber_voice, rate=-2)
    voice("Puis-je vous aider ?", "ood_help", ood_voice, rate=-2)
    voice("Nous sommes les Ood.", "ood_ambient", ood_voice, rate=-2)
    voice("Sontar, ha !", "sontarian_ha", cyber_voice, rate=0)
    voice("Affirmatif, maître.", "k9_affirm", dalek_voice, rate=1)
