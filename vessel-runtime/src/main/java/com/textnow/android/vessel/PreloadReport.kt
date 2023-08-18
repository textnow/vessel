package com.textnow.android.vessel

/**
 * The result of calling [Vessel.preload]/[Vessel.preloadBlocking].
 *
 * This indicates which errors occurred during preloading and whether the
 * preload timeout was hit (if specified)
 *
 * This does not mean preloading failed entirely - any valid data will still be preloaded.
 */
data class PreloadReport(
    /**
     * List of types for which there was no type definition found at runtime.
     *
     * This can occur when a type is removed from code that uses Vessel, but an instance
     * of that type remains stored in Vessel.
     */
    var missingTypes: MutableList<String> = mutableListOf(),

    /**
     * List of types where de-serialization failed.
     *
     * That is, an instance of a type stored in Vessel could not be converted into the current
     * representation of that type.
     *
     * This can occur when the definition of a type is changed, but an instance of the old version of
     * that type remains stored in Vessel.
     *
     * See [Vessel.replace], which can be used to instead swap one type for another, which
     * is a technique that avoids this problem.  For example - rather than changing "MyData",
     * define a new type "MyDataV2" and replace any instance of "MyData" in Vessel with "MyData2"
     */
    var deserializationErrors: MutableList<String> = mutableListOf(),

    /**
     * True if the preload timeout was exceeded (see timeoueMS in [Vessel.preload])
     */
    var timedOut: Boolean = false,

    /**
     * Indicates if one or more errors occurred during preloading:
     * - missing types
     * - deserializaiton errors
     * - preload timeout exceeded
     */
    var errorsOcurred: Boolean = false,
)