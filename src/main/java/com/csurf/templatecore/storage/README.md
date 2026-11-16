# Template Storage Design

This document outlines the design decisions and implementation details for the template storage system in SmartPicTemplate.

## Overview

The storage system uses a hybrid approach that combines:
1. File-based storage for persistence
2. In-memory caching for performance
3. Message-based communication for state updates

## Design Decisions

### 1. File Format: JSON
- **Why JSON?**
  - Human-readable format makes debugging easier
  - Wide tool support for viewing and editing
  - Built-in Java libraries (Jackson) for serialization
  - Flexible schema allows for future extensions

### 2. Directory Structure
- Templates stored in user-specific directory
- Path managed by StorageConfig class
- Development and production paths kept separate
- Directory structure:
  ```
  dev_storage/
    └── templates/
        └── {template_name}.json
  ```

### 3. Template Storage Class
- **Responsibilities:**
  - Manages file I/O operations
  - Handles serialization/deserialization
  - Notifies system of storage events
  - Provides error handling and recovery

### 4. Communication Pattern
- Uses ApplicationMediator for loose coupling
- Messages sent on save/load operations:
  - TEMPLATE_SAVED
  - TEMPLATE_LOADED
- Allows UI components to update without direct coupling

### 5. Error Handling
- Failed operations return null rather than throwing exceptions
- Errors logged to system error stream
- Directory creation handled automatically
- Missing files handled gracefully

## Implementation Details

### Key Classes
1. `TemplateStorage`
   - Main facade for storage operations
   - Manages ObjectMapper configuration
   - Handles file operations

2. `StorageConfig`
   - Manages storage paths
   - Provides environment-specific configurations
   - Ensures directory existence

### Data Flow
1. **Saving:**
   ```
   Template → JSON → File System
   ↓
   Message to UI
   ```

2. **Loading:**
   ```
   File System → JSON → Template
   ↓
   Message to UI
   ```

## Future Considerations

1. **Potential Enhancements:**
   - Version control for templates
   - Template metadata support
   - Backup/restore functionality
   - Cloud storage integration

2. **Migration Path:**
   - Schema versioning support
   - Backward compatibility handling
   - Data validation layer

## Testing Strategy

Testing focuses on:
1. File operations
2. Message delivery
3. Error handling
4. Path configuration
5. Data integrity

## Security Considerations

- Files stored in user space only
- No sensitive data in templates
- Path validation to prevent directory traversal
- Error messages don't expose system details
