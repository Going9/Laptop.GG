# Laptop.GG

## 1. 프로젝트 목표
+ 노트북 및 IT 제품을 좋아하는 IT 덕후로서, 특히 노트북을 잘 아는 본인이 노트북 선택에 어려움이 있는 사람들을 위해 노트북 추천 사이트 개발

## 2. 프로젝트 구조
+ kotlin + spring boot + thymeleaf 사용
+ 현재 jdk 17 사용중이나 추후 21로 올려서 추천 요청 처리를 virtual thread를 사용할 예정

## 3. 구성환경
+ 구형 노트북에 Ubuntu Server 설치
  + 인텔 i3-6100 노트북용 저전력 cpu(tdp 15w)
  + 2코어 4스레드, 2GB RAM, 256GB SSD
+ iptime 공유기에 유선으로 물려놓음
  + 고정 IP가 없으나 iptime 공유기 자체적인 DDNS 기능을 활용
+ GitHub Action으로 레파지토리 연동 완료
+ 임시 도메인 laptopgg.kro.kr에 등록 완료

## 4. 진행현황
+ CPU, GPU, LAPTOP 생성 기능 및 페이지 완성
  + 타임 리프에서 중첩된 dto의 val 필드를 제대로 인식하지 못하는 문제 발생하여 현재 var로 적용
+ 추천을 위한 reqeust dto 임시 형태 완성
+ 추천 로직은 필터링 + 가중치로 예상중
