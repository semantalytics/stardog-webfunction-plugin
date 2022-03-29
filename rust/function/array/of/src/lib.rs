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

extern {
        pub fn mappingDictionaryGet(buf_addr: i64) -> i32;
}

#[no_mangle]
pub extern fn evaluate(args: *mut c_char) -> *mut c_char {
    let args_str = unsafe { CStr::from_ptr(args).to_str().unwrap() };

    let sparql_results: Value = serde_json::from_str(args_str).unwrap();
    let values = sparql_results["results"]["bindings"][0];

    let output = json!({
      "head": {"vars":["result"]}, "results":{"bindings":[{"result":{"type":"literal","value": ""}}]}
    }).to_string().as_bytes().to_vec();

    unsafe { CString::from_vec_unchecked(output) }.into_raw()

}

#[no_mangle]
pub extern fn doc() -> *mut c_char {

    let output = b"
function description here

arguemnts:
    value_0:literal first string to compare
    value_1:literal second string to compare

	".to_vec();

    unsafe { CString::from_vec_unchecked(output) }.into_raw()
}
