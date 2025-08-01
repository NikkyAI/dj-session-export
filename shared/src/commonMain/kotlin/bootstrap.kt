//@file:OptIn(ExperimentalForeignApi::class)
//import kotlinx.cinterop.*
//import platform.windows.*
//import platform.posix.*
//import kotlin.math.max
//
//typealias WSTR = CPointer<UShortVar>
//
//private fun WSTR.toKString(): String = memScoped {
//    // Figure out how much memory we need after UTF-8 conversion.
//    val sz = WideCharToMultiByte(CP_UTF8, WC_ERR_INVALID_CHARS, this@toKString, -1, null, 0, null, null)
//    // Now convert to UTF-8 and from there, a String.
//    val utf8 = allocArray<ByteVar>(sz)
//    val r = WideCharToMultiByte(CP_UTF8, WC_ERR_INVALID_CHARS, this@toKString, -1, utf8, sz, null, null)
//    if (r == 0) throw RuntimeException("Could not convert to UTF-8")
//    utf8.toKString()
//}
//
//private val fullBinaryPath: String by lazy {
//    // Get the path to the EXE.
//    val hmodule = GetModuleHandleW(null)
//    val wstr: WSTR = nativeHeap.allocArray<UShortVar>(MAX_PATH)
//    GetModuleFileNameW(hmodule, wstr, MAX_PATH)
//    // Strip the filename leaving just the directory.
//    PathRemoveFileSpecW(wstr)
//    wstr.toKString()
//}