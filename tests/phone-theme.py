"""Physical QA regression: interactive theme selection and persisted app restarts.

Run with Pillow installed, the test phone connected and system night mode enabled.
Does not change the device's system theme or production app preferences.
"""
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image

ADB = 'C:/Users/marti/AppData/Local/Android/Sdk/platform-tools/adb.exe'
night = subprocess.check_output([ADB, '-s', 'bd36348a', 'shell', 'cmd', 'uimode', 'night'], text=True)
assert 'yes' in night, 'This regression requires the phone system theme to be dark.'

def run(*args):
    subprocess.run(['node', 'tests/phone-confirmation.cjs', *args], check=True, capture_output=True, timeout=45)

def expect_background(name, expected):
    run('screen', name)
    with Image.open(Path('output/confirmation-phone') / (name + '.png')) as image:
        actual = image.convert('RGB').getpixel((10, image.height // 2))
    assert actual == expected, f'{name}: expected canvas {expected}, observed {actual}'

run('open', 'home', 'es')
for label, expected, name, preference in [
    ('Claro', (243, 245, 248), 'light', 'claro'),
    ('Oscuro', (16, 27, 41), 'dark', 'oscuro'),
    ('Según el sistema', (16, 27, 41), 'system', 'sistema'),
]:
    run('tap', 'Ajustes')
    run('tap', 'Tema')
    run('tap', label)
    expect_background('theme-' + name, expected)
    run('open', 'home', 'es')
    expect_background('theme-' + name + '-reopened', expected)
    saved = subprocess.check_output([ADB, '-s', 'bd36348a', 'shell', 'run-as',
        'com.martincornelli.agendasacramental.design.qa', 'cat', 'shared_prefs/agenda_prefs.xml'], text=True)
    assert ET.fromstring(saved).find("string[@name='tema']").text == preference
    print(f'{label}: applies immediately and persists after restart', flush=True)
