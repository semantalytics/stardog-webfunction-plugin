use std::ffi::{CStr, CString}; 
use std::mem; 
use std::os::raw::{c_char, c_void}; 
use serde_json::{Value, json};
use strsim::normalized_levenshtein;

pub use stardog_function::*;

#[no_mangle]
pub extern fn evaluate(subject: *mut c_char) -> *mut c_char {
    let subject = unsafe { CStr::from_ptr(subject).to_str().unwrap() };

    let v: Value = serde_json::from_str(subject).unwrap();
    let str1 = v["results"]["bindings"][0]["value_1"]["value"].as_str().unwrap();
    let str2 = v["results"]["bindings"][0]["value_2"]["value"].as_str().unwrap();

    let result = normalized_levenshtein(str1, str2);

    let sparql_query_result = json!({
      "head": {"vars":["result"]}, "results":{"bindings":[{"result":{"type":"literal","value": result}}]}
    }).to_string();

    unsafe { CString::from_vec_unchecked(sparql_query_result) }.into_raw()

}

#[no_mangle]
pub extern fn doc() -> *mut c_char {

    let mut output = b"".to_vec();
    let result = "This is documentation blah, blah, blah";

    output.extend(json!({
      "head": {"vars":["result"]}, "results":{"bindings":[{"result":{"type":"literal","value": result}}]}
    }).to_string().bytes());

    unsafe { CString::from_vec_unchecked(output) }.into_raw()
}

