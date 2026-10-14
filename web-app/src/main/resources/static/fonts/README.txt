LaptopGG UI Sans: NanumGothic Regular (fonts-nanum Ubuntu package 20200506-1).
Source: https://hangeul.naver.com/font
Copyright 2010 NAVER Corporation, SIL Open Font License 1.1; see LICENSE.txt.
Converted losslessly from TTF to WOFF2 using FontTools; renamed the family to
LaptopGG UI Sans to respect the reserved font names. Glyphs were not subset.
Served locally so Korean typography does not depend on third-party requests.
The Korean caption in image-placeholder.svg is outlined from the same font
so SVG image rendering does not depend on installed operating-system fonts.
Removed optional VTT source (TSI*) tables: the upstream empty TSI3 table is
rejected by Chromium's OTS sanitizer. Runtime glyphs/hinting remain intact.
UI tests assert the local Korean font actually loads in both browsers.
