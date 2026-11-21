/**
 * Data passed from services to the Swing screens.
 * <ul>
 *   <li>Every DTO is an immutable Java record.</li>
 *   <li>A DTO never contains a password hash or any other secret.</li>
 *   <li>Screens only receive DTOs; {@code model} records never reach Swing code.</li>
 * </ul>
 */
package com.campusgigs.dto;
