use std::ffi::{CStr, CString}; 
use std::mem; 
use std::os::raw::{c_char, c_void}; 
use serde_json::{Value, json};

pub use stardog_function::*;

#[no_mangle]
pub extern fn evaluate(subject: *mut c_char) -> *mut c_char {
    let subject = unsafe { CStr::from_ptr(subject).to_str().unwrap() };

    let v: Value = serde_json::from_str(subject).unwrap();

    let result:Vec<&str> = v["results"]["bindings"][0]["value_1"]["value"].as_str().unwrap().trim_matches(|c| c == '[' || c == ']').split(",").collect();

    let output_ptr = unsafe { mappingDictionaryGet(result[0].trim().parse::<i64>().unwrap()) };
    let output_str = unsafe { CStr::from_ptr(output_ptr).to_str().unwrap() };

    let output_from_map_get_json: Value = serde_json::from_str(output_str).unwrap();
    let output_value = output_from_map_get_json["results"]["bindings"][0]["value_0"]["value"].as_str().unwrap().to_uppercase();

//    let output_value = "woohoo";
    let mut r = b"".to_vec();

    r.extend(json!({
      "head": {"vars":["result"]}, "results":{"bindings":[{"result":{"type":"literal","value": output_value}}]}}).to_string().bytes());

    return unsafe { CString::from_vec_unchecked(r) }.into_raw();
}
