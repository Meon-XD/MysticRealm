# Joyfull RPG — 2D Top-Down Fantasy Asset Pack

Paket konsep visual awal untuk game RPG 2D dengan sudut pandang top-down / drone view.

## Isi paket

- `06_Dokumentasi/Preview_Asset_Atlas_01.png` — atlas konsep besar: roster class, monster, boss, bioma, tileset, bangunan, dekorasi, objek interaktif, UI, dan efek.
- `06_Dokumentasi/Preview_Asset_Atlas_02.png` — atlas konsep tambahan: variasi class, musuh, struktur, area spesial, contoh map, ground tiles, dan UI.
- Folder kategori disiapkan untuk pemisahan aset final: karakter, monster/boss, area, tileset/dekorasi, UI/efek.

## Roster karakter yang dituju

Warrior, Mage, Assassin, Archer, Knight, Priest, Rogue, Hunter, Necromancer, Alchemist, Merchant, Bard, Engineer, Shaman, Ninja, Thief, Druid, Ranger, Paladin, Summoner, Guardian, Dragon Knight, Dancer, Beast Tamer, Gunslinger, Monk, Samurai, Lancer, Pirate, Tinkerer, dan kelas rahasia.

## Kategori monster

Slime elemental, kelelawar, serigala, goblin, orc, skeleton, zombie, spider, scorpion, bandit, golem, mimic, mushroom creature, plant monster, ghost, wraith, demon, wyvern, dragon, undead knight, elemental, sea monster, ancient guardian, dan boss dungeon.

## Area / level

Desa pemula, kota dagang, hutan hijau, hutan terkutuk, padang rumput, pantai, pulau tropis, gurun, oasis, gunung salju, gua kristal, tambang, reruntuhan, kuil, dungeon batu, kastil, rawa, gunung berapi, neraka, dimensi astral, pulau langit, dan sarang naga.

## Dekorasi dan objek

Pohon, semak, bunga, batu, jamur, pagar, jalan, jembatan, lampu, obor, peti, tong, meja, kursi, kios, rumah, menara, patung, altar, portal, kristal, air mancur, reruntuhan, gerbang, gerobak, tenda, penanda jalan, dan objek interaktif.

## Catatan produksi penting

File PNG di folder dokumentasi adalah **lembar konsep/preview gabungan**, bukan sprite individual transparan. Elemen di dalam atlas belum dipotong menjadi file terpisah, belum memiliki animasi sprite-sheet per karakter, dan belum dijamin memiliki ukuran tile/pivot/collision yang siap impor ke engine. Teks kecil dan detail atlas juga sebaiknya dipakai sebagai referensi visual, bukan sebagai aset final langsung.

Untuk produksi game, tahap berikutnya adalah memisahkan tiap objek menjadi PNG transparan, menyamakan ukuran grid (misalnya tile 32×32 atau 48×48), membuat animasi idle/walk/attack/hurt/death, menentukan pivot dan collision, lalu menyiapkan atlas terpisah untuk Godot/Unity/GameMaker.

## Gaya visual

2D fantasy pixel-art, top-down / 3⁄4 view, palet warna kaya, siluet mudah dibaca, outline konsisten, dan proporsi karakter chibi.

## Penggunaan

Konsep visual ini dibuat untuk proyek RPG pengguna. Periksa dan sesuaikan detail sebelum distribusi komersial; pastikan aset final dipisah, dirapikan, dan diuji di game engine.
