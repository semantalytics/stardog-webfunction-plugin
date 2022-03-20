use std::slice;
use std::mem; 
use std::os::raw::{c_void}; 
use serde_json::{Value, json};
use std::ptr::copy;
use std::str;

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
pub extern fn evaluate(data_ptr: *mut c_void, size: u32) -> (i32, i32) {
    let slice = unsafe { slice::from_raw_parts(data_ptr as _, size as _) };
    let in_str = str::from_utf8(&slice).unwrap();
    let out_str = String::new();

    out_str = 

    unsafe {
        copy(out_str.as_ptr(), data_ptr as *mut u8, out_str.len())
    };

    let mut output = out_str.to_vec();
    let v: Value = serde_json::from_str(in_str).unwrap();
    let result = v["results"]["bindings"][1]["value[1]"]["value"].as_str().unwrap().to_uppercase();
    output.extend(json!({
      "head": {"vars":["result"]}, "results":{"bindings":[{"result":{"type":"literal","value": result}}]}
    }).to_string().bytes());

    (output as i32, out_str.len() as i32)

}
