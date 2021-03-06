use std::ffi::{CStr, CString}; 
use std::os::raw::c_char;
use serde_json::{Value, json};

extern {
    pub fn mappingDictionaryAdd(buf_addr: i32) -> i64;
}

extern {
    pub fn mappingDictionaryGet(buf_addr: i64) -> *mut c_char;
}

#[no_mangle]
pub extern fn evaluate(subject: *mut c_char) -> *mut c_char {
    let subject = unsafe { CStr::from_ptr(subject).to_str().unwrap() };

    let values: Value = serde_json::from_str(subject).unwrap();
    let value_0 = values["results"]["bindings"][0]["value_0"]["value"].as_str().unwrap();

    let result = value_0.to_uppercase();

    let result = json!({
      "head": {"vars":["value_0"]}, "results":{"bindings":[{"value_0":{"type":"literal","value": result}}]}
    }).to_string().into_bytes();

    unsafe { CString::from_vec_unchecked(result) }.into_raw()

}
