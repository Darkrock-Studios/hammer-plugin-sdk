//! `words` splits text as the C and Kotlin kits do.

use crate::{Word, words};

fn texts(text: &str) -> Vec<&str> {
    words(text).map(|word| word.text).collect()
}

#[test]
fn words_are_runs_of_letters_with_apostrophes_between_letters() {
    assert_eq!(texts("Don't say l'arbre — rock’n’roll! ʼtwas"), ["Don't", "say", "l'arbre", "rock’n’roll", "ʼtwas"]);
    assert_eq!(texts("very every, “Very.”"), ["very", "every", "Very"]);
}

#[test]
fn digits_hyphens_and_trailing_apostrophes_end_a_word() {
    assert_eq!(texts("well-known abc123def dogs' "), ["well", "known", "abc", "def", "dogs"]);
    assert!(texts("  42 — … ").is_empty());
}

#[test]
fn offsets_are_utf8_bytes() {
    let text = "Ça va, très bien.";
    let found: Vec<Word> = words(text).collect();
    assert_eq!(
        found,
        [
            Word { text: "Ça", start: 0, end: 3 },
            Word { text: "va", start: 4, end: 6 },
            Word { text: "très", start: 8, end: 13 },
            Word { text: "bien", start: 14, end: 18 },
        ]
    );
    found.iter().for_each(|word| assert_eq!(&text[word.start..word.end], word.text));
}
