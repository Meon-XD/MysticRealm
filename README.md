# Mystic Realm

Android Action RPG + teka-teki, Dibuat Pake Bahasa Java, jadi ya begitulah.

## Build

Kebutuhan Termux:

```bash
pkg install aapt2 openjdk-21 d8 apksigner zip
```

Jalankan:

```bash
cd /storage/emulated/0/apk/GAME_RPG/MysticRealm
bash build.sh
```

Hasil build:

- `MysticRealm/MysticRealm.apk`
- salinan di `/storage/emulated/0/apk/GAME_RPG/MysticRealm.apk`

Keystore default memakai `/storage/emulated/0/apk/parental apk/debug.keystore`
dengan alias `monitor`. Password wajib diberikan lewat environment:

```bash
KEYSTORE=/path/debug.keystore KS_PASS=... KS_ALIAS=... bash build.sh
```

## Instal

```bash
adb install -r /storage/emulated/0/apk/GAME_RPG/MysticRealm.apk
```

Atau buka file APK dari pengelola berkas Android.

## Kontrol

- Joystick kiri bawah: bergerak.
- Tombol kanan bawah: serang, skill 1-3, dash, ramuan, jeda.
- Serangan dan skill otomatis membidik musuh terdekat; bidikan mouse tetap diprioritaskan.
- Sentuh menu/panel untuk memilih.
- Dari peta dunia tersedia tombol Inventaris, Toko, dan Skill.
- Tombol kembali Android: kembali satu layar; dari menu utama keluar aplikasi.

## Alur Permainan

1. Pilih kelas: Warrior, Mage, atau Assassin.
2. Peta dunia berisi 5 dunia x 5 stage.
3. Kalahkan semua musuh, lalu sentuh altar untuk memulai teka-teki.
4. Teka-teki berhasil membuka stage; gagal memunculkan musuh tambahan.
5. Stage 5 setiap dunia memunculkan bos setelah segel terbuka.
6. Naik level mendapat poin untuk skill dan talenta.
7. Item, gold, shard, equipment, dan progres tersimpan otomatis.

Fitur: 3 kelas, 7 tipe monster, bos unik per dunia, 8 tipe teka-teki,
inventaris, toko, upgrade equipment, skill, talenta, 5 tingkat kesulitan,
pengaturan volume musik/SFX, ukuran tombol, sensitivitas analog, kualitas grafis,
musik/SFX sintetis, minimap, penyimpanan lokal, serta sinematik pembuka dan penutup.

Visual gameplay memakai renderer Canvas 2D bergaya 2.5D, bukan model 3D.
Arena kini memakai variasi tekstur dan detail per biome, siluet pohon/batu
lebih berstruktur, gerakan karakter bermuatan langkah, musuh dengan animasi
melayang yang berbeda, dan tombol aksi dengan highlight serta status siap skill.

## Sinematik

- Video pembuka diputar saat memulai petualangan baru; tersedia tombol `Lewati`.
- Video penutup diputar setelah bos terakhir dikalahkan.
- Video 720p tersimpan di `res/raw/cinematic_opening.mp4` dan
  `res/raw/cinematic_ending.mp4`.

## Struktur

- `src/com/mysticrealm/MainActivity.java` - activity dan lifecycle.
- `CinematicDialog.java` - pemutar video pembuka/penutup layar penuh.
- `GameView.java` - loop frame dan input sentuh.
- `Game.java` - mode layar, simpan/muat, transisi.
- `Screens.java` - menu, peta, inventory, toko, skill, pengaturan, hasil.
- `Gameplay.java` - pertempuran, kamera, HUD, altar, bos.
- `Player.java`, `Enemy.java`, `World.java`, `Puzzle.java` - gameplay.
- `Catalog.java` - seluruh data kelas, item, monster, bos, teka-teki.
- `SaveData.java` - progres JSON di SharedPreferences.
- `Gfx.java` - primitif gambar dan teks Canvas.
- `Audio.java` - sintesis musik dan SFX.
