use std::ffi::CString;
use std::mem; 
use std::os::raw::{c_char, c_void}; 
use serde_json::json;
use std::f64::consts;

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
pub extern fn evaluate(_subject: *mut c_char) -> *mut c_char {

    let result = json!({
      "head": {"vars":["result"]}, "results":{"bindings":[{"result":{"type":"literal","value": consts::LOG2_10}}]}
    }).to_string().into_bytes();

    return unsafe { CString::from_vec_unchecked(result) }.into_raw();
}