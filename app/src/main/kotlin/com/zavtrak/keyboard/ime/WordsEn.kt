package com.zavtrak.keyboard.ime

/** Top ~450 English words with rough frequency weights, used for suggestions/autocorrect. */
internal object WordsEn {
    val list: List<Pair<String, Int>> = """
        the 1000 of 950 and 940 to 930 a 920 in 900 is 890 it 880 you 870 that 860
        he 850 was 840 for 830 on 820 are 810 as 800 with 790 his 780 they 770 i 760
        at 750 be 740 this 730 have 720 from 710 or 700 one 690 had 680 by 670 word 660
        but 650 not 640 what 630 all 620 were 610 we 600 when 590 your 580 can 570 said 560
        there 550 use 540 an 530 each 520 which 510 she 500 do 490 how 480 their 470 if 460
        will 450 up 440 other 430 about 420 out 410 many 400 then 390 them 380 these 370 so 360
        some 350 her 340 would 330 make 320 like 310 him 300 into 290 time 280 has 270 look 260
        two 250 more 240 write 230 go 220 see 210 number 200 no 190 way 180 could 170 people 160
        my 155 than 150 first 145 water 140 been 135 call 130 who 125 oil 120 its 115 now 110
        find 108 long 106 down 104 day 102 did 100 get 99 come 98 made 97 may 96 part 95
        over 94 new 93 sound 92 take 91 only 90 little 89 work 88 know 87 place 86 year 85
        live 84 me 83 back 82 give 81 most 80 very 79 after 78 thing 77 our 76 just 75
        name 74 good 73 sentence 72 man 71 think 70 say 69 great 68 where 67 help 66 through 65
        much 64 before 63 line 62 right 61 too 60 mean 59 old 58 any 57 same 56 tell 55
        boy 54 follow 53 came 52 want 51 show 50 also 49 around 48 form 47 three 46 small 45
        set 44 put 43 end 42 why 41 again 40 turn 39 here 38 off 37 went 36 old 35
        such 34 because 33 ask 32 men 31 read 30 need 29 land 28 different 27 home 26 us 25
        move 24 try 23 kind 22 hand 21 picture 20 again 19 change 18 air 17 away 16 animal 15
        house 14 point 13 page 12 letter 11 mother 10 answer 9 found 8 study 7 still 6 learn 5
        should 5 america 5 world 5 high 5 every 5 near 5 add 5 food 5 between 4 own 4
        below 4 country 4 plant 4 last 4 school 4 father 4 keep 4 tree 4 never 4 start 4
        city 4 earth 4 eye 4 light 4 thought 4 head 4 under 4 story 4 saw 4 left 4
        few 4 while 4 along 4 might 4 close 4 something 4 seem 4 next 4 hard 4 open 4
        example 4 begin 4 life 4 always 4 those 4 both 4 paper 4 together 3 got 3 group 3
        often 3 run 3 important 3 until 3 children 3 side 3 feet 3 car 3 mile 3 night 3
        walk 3 white 3 sea 3 began 3 grow 3 took 3 river 3 four 3 carry 3 state 3
        once 3 book 3 hear 3 stop 3 without 3 second 3 later 3 miss 3 idea 3 enough 3
        eat 3 face 3 watch 3 far 3 indian 3 real 3 almost 3 above 3 girl 3 sometimes 3
        mountain 3 cut 3 young 3 talk 3 soon 3 list 3 song 3 being 3 leave 3 family 3
        ill 3 happy 3 happen 3 kid 3 build 3 self 3 earth 3 father 3 head 3 stand 3
        page 3 letter 3 mother 3 answer 3 found 3 study 3 still 3 learn 3
        hello 5 world 5 keyboard 4 android 4 phone 4 mobile 3 fast 3 quick 3 brown 3 fox 3
        jumps 3 over 3 lazy 3 dog 3 morning 3 evening 3 night 3 tonight 3 tomorrow 3 yesterday 3
        today 5 please 5 thanks 5 thank 5 sorry 4 maybe 4 perhaps 3 actually 3 really 4 sure 4
        okay 4 going 4 done 4 doing 4 made 4 making 4 great 4 awesome 3 nice 4 cool 4
        hey 5 yeah 5 nope 4 yes 5 alright 4 message 4 reply 3 send 4 received 3 missing 3
        love 4 like 4 hate 3 friend 4 friends 4 family 4 mother 3 father 3 brother 3 sister 3
        coffee 3 tea 3 water 3 lunch 3 dinner 3 breakfast 3 hungry 3 tired 3 bored 3 happy 3
        meeting 3 project 3 task 3 deadline 3 idea 3 problem 3 solve 3 question 3 answer 3
        github 4 commit 3 branch 3 merge 3 review 3 deploy 3 build 4 release 3 docker 3
        kotlin 3 java 3 python 3 typescript 3 javascript 3 react 3 compose 3 swift 3
    """.trimIndent()
        .replace("\n", " ")
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .chunked(2)
        .mapNotNull { pair ->
            if (pair.size != 2) return@mapNotNull null
            val w = pair[0].lowercase()
            val f = pair[1].toIntOrNull() ?: return@mapNotNull null
            w to f
        }
        .distinctBy { it.first }
}
