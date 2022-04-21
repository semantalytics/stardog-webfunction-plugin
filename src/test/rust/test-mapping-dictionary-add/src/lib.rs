use std::ffi::{CStr, CString}; 
use std::mem; 
use std::os::raw::{c_char, c_void}; 
use serde_json::{Value, json};

pub use stardog_function::*;

#[no_mangle]
pub extern fn evaluate(subject: *mut c_char) -> *mut c_char {
    let subject = unsafe { CStr::from_ptr(subject).to_str().unwrap() };

    let mut output = b"".to_vec();
    let v: Value = serde_json::from_str(subject).unwrap();

    let result = v["results"]["bindings"][0]["value_0"]["value"].as_str().unwrap().to_uppercase();

    output.extend(json!({
      "head": {"vars":["value_0"]}, "results":{"bindings":[{"value_0":{"type":"literal","value": result}}]}
    }).to_string().bytes());

    let output_ptr = unsafe { CString::from_vec_unchecked(output) }.into_raw();

    let id = unsafe { mappingDictionaryAdd(output_ptr as i32) };
    let mut r = b"".to_vec();

    r.extend(json!({
      "head": {"vars":["value_0"]}, "results":{"bindings":[{"value_0":{"type":"literal","value": format!("[{}]", id), "datatype": "tag:stardog:api:array"}}]}
    }).to_string().bytes());

    return unsafe { CString::from_vec_unchecked(r) }.into_raw();
}
