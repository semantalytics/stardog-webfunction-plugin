use std::ffi::{CStr, CString}; 
use std::mem; 
use std::os::raw::{c_char, c_void}; 
use serde_json::{Value, json};

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

extern {
	pub fn mappingDictionaryAdd(buf_addr: i32) -> i64;
}

#[no_mangle]
pub extern fn evaluate(subject: *mut c_char) -> *mut c_char {
    let subject = unsafe { CStr::from_ptr(subject).to_str().unwrap() };

    let mut output = b"".to_vec();
    let v: Value = serde_json::from_str(subject).unwrap();

    let result = v["results"]["bindings"][0]["value_1"]["value"].as_str().unwrap().to_uppercase();

    output.extend(json!({
      "head": {"vars":["result"]}, "results":{"bindings":[{"result":{"type":"literal","value": result}}]}
    }).to_string().bytes());

    let output_ptr = unsafe { CString::from_vec_unchecked(output) }.into_raw();

    let id = unsafe { mappingDictionaryAdd(output_ptr as i32) };
    let mut r = b"".to_vec();

    r.extend(json!({
      "head": {"vars":["result"]}, "results":{"bindings":[{"result":{"type":"literal","value": format!("[{}]", id), "datatype": "tag:stardog:api:array"}}]}
    }).to_string().bytes());

    return unsafe { CString::from_vec_unchecked(r) }.into_raw();
}
