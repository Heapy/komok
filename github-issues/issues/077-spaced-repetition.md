# #77 Spaced Repetition

- State: open
- Author: @IRus
- Created: 2021-07-07T11:34:10Z
- Updated: 2021-07-09T16:25:47Z
- Closed: none
- Labels: none
- Assignees: @IRus
- Milestone: none
- Comments: 1
- URL: https://github.com/Heapy/komok/issues/77

## Body

> Did you know that you will forget almost 90% of new information within a week if you don’t repeat it?

Repeat important information

## Comments (1)

### Comment 1 by @madhead at 2021-07-09T16:25:47Z

URL: https://github.com/Heapy/komok/issues/77#issuecomment-877305889

My dream also, to make a killer of Anki, so I'll double that. Where do I contribute, though?

1. Anki has absolutely non-portable collection (cards, decks, media) format - it's just an SQLite database without clear separation of immutable data (facts itself) and mutable user-specific data (SRS guts, i.e. coefficients and dates). You cannot easily sync a collection across devices or share your collection.
1. So there is no way to quickly add cards. E.g. when I'm learning new language I would like to:
  a. Eitheir get a quality deck from the community or store
  b. Create my own based on a language corpus. Here I would like to use my coding skills to atutomate the process. With Anki it's impossible.
1. And it makes maintaining the decks impossible. The best deck for English language is 12500 words, and.. there is no source for it. If I want to add my own words there (fork) or fix some translations (contribute).. I cannot, it's impossible.
1. AnkiWeb (a tool to sync your data and share collections) is... a piece of crap from 2001: no social stuff, no visibility of your data (it "just works", but you cannot check what collections are there in what state). And most importantly it's future is unclear. If one day it stops working, or I exceed some limits (they are not defined, though) - I will loose my data, which, in case of SRS is crucial.
1. AnkiDroid targets pre-historic Androids (e.g. they supported Android 2.x when there was already 5.x or 6.x) - this legacy made it impossible to contribute some features, like timely reminders. Believe me, I did some contributions trying to make it more... modern.
1. AnkiDroid's core [is a line-by-line port of Supermemo 17 from Python to Java](https://groups.google.com/g/anki-android/c/6NASdjus0n0/m/vZxgPZIYAxwJ) (at least it was in 2015 and I don't think anything changed a lot from then). The whitepaper for SM alghorithms doesn't look so hard to understand and reimplement in a more portable and efficient way (i.e. KMM :)). Because a Python port was a deathtrap for a developer.
1. Anki for iOS costs like what? 25 dollars? Totally overpriced!
1. Anki's model for cards (HTML) is too powerful. It's hard for users to create their own cards (it's "geeky") and it's hard to render.


Yet Anki is still the best program for SRS :(
