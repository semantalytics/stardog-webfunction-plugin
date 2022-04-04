use std::ffi::{CStr, CString}; 
use std::mem; 
use std::os::raw::{c_char, c_void}; 
use serde_json::{Value, json};
use lingua::{Language, LanguageDetector, LanguageDetectorBuilder};
use lingua::Language::{English, French, German, Spanish};

#[no_mangle]
pub extern fn malloc(size: usize) -> *mut c_void {
    let mut buffer = Vec::with_capacity(size);
    let pointer = buffer.as_mut_ptr();
    mem::forget(buffer);

    pointer as *mut c_void
}

#[no_mangle]
pub extern fn free(pointer: *mut c_void, capacity: usize) {
    unsafe {
        let _ = Vec::from_raw_parts(pointer, 0, capacity);
    }
}

#[no_mangle]
pub extern fn evaluate(subject: *mut c_char) -> *mut c_char {
    let subject = unsafe { CStr::from_ptr(subject).to_str().unwrap() };

    let mut output = b"".to_vec();
    let v: Value = serde_json::from_str(subject).unwrap();
    let value_1 = v["results"]["bindings"][0]["value_1"]["value"].as_str().unwrap();

    let detector: LanguageDetector = LanguageDetectorBuilder::from_all_languages().with_preloaded_language_models().build();
    let detected_language: Option<Language> = detector.detect_language_of(value_1);

    output.extend(json!({
      "head": {"vars":["result"]}, "results":{"bindings":[{"result":{"type":"literal","value": detected_language}}]}
    }).to_string().bytes());

    unsafe { CString::from_vec_unchecked(output) }.into_raw()

}

