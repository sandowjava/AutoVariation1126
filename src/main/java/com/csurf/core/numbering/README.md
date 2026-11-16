# Number System Conversion Utility

## Overview

The `NumberingSystemConverter` class provides a standardized way to handle conversions between user-facing numbering systems (1-based) and internal code numbering systems (0-based). This utility addresses a fundamental disconnect between how users think about numbers and how programming languages handle array indices and other numerical sequences.

## Problem Statement

1. **User vs Code Mental Models**
   - Users naturally think in terms of numbers starting at 1
   - Programming languages and arrays use 0-based indexing
   - This disconnect leads to off-by-one errors and confusion

2. **Legacy Code Patterns**
   - Historical code often uses arrays with dummy cells at index 0
   - This pattern appears in multiple places:
     * MonoTriPage fill order numbers (1-78 vs 0-77)
     * Hue number systems (1-36 vs 0-35)
   - Need for consistent handling of these patterns

## Solution

The `NumberingSystemConverter` provides:

1. **Explicit Conversion Methods**
   - `convertUserNumberToCodeNumber`: Converts from user (1-based) to code (0-based)
   - `convertCodeNumberToUserNumber`: Converts from code (0-based) to user (1-based)

2. **Domain-Specific Validation**
   - Each numbering domain (hue numbers, fill orders, etc.) has its own valid ranges
   - Built-in validation ensures numbers stay within domain-specific bounds

3. **Legacy Support**
   - Utilities for creating arrays with dummy cells
   - Maintains compatibility with existing code patterns

## Usage Guidelines

1. Use this converter whenever translating between:
   - User interface numbers and internal code numbers
   - Legacy array indices and modern 0-based indices

2. Always explicitly document which numbering system you're using:
   - User-facing (1-based) numbers in UI code
   - Code (0-based) numbers in internal logic

3. Consider adding new domains through the `NumberingDomain` enum when:
   - You have a new number sequence that users interact with
   - You find legacy code using 1-based numbering

## Benefits

1. **Clarity**
   - Makes the conversion between numbering systems explicit
   - Self-documenting code through clear method names
   - Reduces confusion about number ranges

2. **Consistency**
   - Standardized way to handle number system conversions
   - Centralized validation of number ranges
   - Single source of truth for conversion logic

3. **Maintainability**
   - Easier to find and fix numbering-related bugs
   - Simpler to modify number ranges if needed
   - Clear separation between user and code number spaces
