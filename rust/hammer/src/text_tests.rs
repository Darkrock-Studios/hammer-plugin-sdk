//! `words` splits the shared cases in tests/words.txt as the C and Kotlin kits do.

use crate::{Word, words};

fn unescape(s: &str) -> String {
    let mut out = String::new();
    let mut rest = s;
    while let Some(at) = rest.find("\\u{") {
        out.push_str(&rest[..at]);
        let end = at + rest[at..].find('}').unwrap();
        out.push(char::from_u32(u32::from_str_radix(&rest[at + 3..end], 16).unwrap()).unwrap());
        rest = &rest[end + 1..];
    }
    out + rest
}

#[test]
fn words_split_the_shared_cases() {
    let cases = include_str!("../../../tests/words.txt");
    for line in cases.lines().filter(|line| !line.is_empty() && !line.starts_with('#')) {
        let (text, expected) = line.split_once('\t').unwrap();
        let (text, expected) = (unescape(text), unescape(expected));
        let found: Vec<Word> = words(&text).collect();
        let expected: Vec<&str> = expected.split(' ').filter(|word| !word.is_empty()).collect();
        assert_eq!(found.iter().map(|word| word.text).collect::<Vec<_>>(), expected, "{line}");
        found.iter().for_each(|word| assert_eq!(&text[word.start..word.end], word.text, "{line}"));
    }
}

#[test]
fn offsets_are_utf8_bytes() {
    assert_eq!(
        words("Ça va, très 𐌰𐌱.").collect::<Vec<_>>(),
        [
            Word { text: "Ça", start: 0, end: 3 },
            Word { text: "va", start: 4, end: 6 },
            Word { text: "très", start: 8, end: 13 },
            Word { text: "𐌰𐌱", start: 14, end: 22 },
        ]
    );
}

#[test]
fn the_ascii_shortcut_agrees_with_the_table() {
    for c in '\0'..='\x7F' {
        let table = crate::word_classes::WORD_CLASSES
            .iter()
            .find(|&&(first, last, _)| (first..=last).contains(&(c as u32)))
            .map_or(0, |&(_, _, class)| class);
        assert_eq!(crate::word_class::of(c), table, "{c:?}");
    }
}
