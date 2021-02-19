# Worms Game : FRJ

## Strategi Greedy

Strategi greedy yang kami pilih yakni membunuh lawan secepat mungkin dengan cara target 1 worm dan dekati lokasinya.

Adapun urutan prioritas command adalah 
- Select worm lain jika, currentWorm sedang terkena freeze, namun khusus jika worm dapat menembak baik bananabomb,snowball ataupun basic shoot
- Select worm lain, jika currentWorm ada commando, dan worm lain dapat menembakan bananabomb untuk agent atau snowball untuk technologist
- Jika ada lawan berada pada jarak tembak, tembak lawan tersebut
- Jika tidak ada lawan yang dapat ditembak, lakukan pergerakan mendekati lawan dengan jarak terpendek
- Jika lawan sudah dekat namun belum berada pada jarak tembak, prioritas pilih jarak tembak terjauh

## Requirement dan instalasi

- Java SE Dev kit 8
- maven untuk build

## Cara menggunakan program

- Untuk menjalankan masuk pada directory bin, kemudian buat file bot.json yang berisi
`{
	"author": "Faris Randy Jafar",
	"email": "FRJ@gmail.com",
	"nickName": "FRJ",
	"botLocation": "/",
	"botFileName": "FRJ.jar",
	"botLanguage": "java"
}`

- jalankan run.bat pada game engine, lakukan konfigurasi juga pada game-runner-config.json
dengan mengedit `"player-a": "./WormsGame/bin",`