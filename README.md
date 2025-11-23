# DevReview
**Secure Staging & Review System for Minecraft Administrative Commands**

## Overview
DevReview is a robust security plugin designed to intercept critical administrative commands (such as `/op`, `/ban`, `/stop`) and place them in a staging area. Authorized staff must review and approve these commands via an interactive GUI before they are executed. This system prevents accidental misuse or malicious abuse of administrative powers, ensuring a safer server environment.
```yaml
# List of commands that require review
critical-commands:
  - "/op"
  - "/deop"
  - "/stop"
  - "/reload"
  - "/restart"
  - "/ban"
  - "/kick"

# Messages
messages:
  command-staged: "§e[DevReview] Command staged for review: %command%"
  bypass-notification: "§c[DevReview] Bypassed review for: %command%"
```

## Commands & Permissions

| Command | Permission | Description |
|---|---|---|
| `/review` | `devreview.admin` | Opens the review GUI to manage pending commands. |
| N/A | `devreview.bypass` | Allows a player to bypass the staging system and execute commands immediately. |
| N/A | `devreview.admin` | Receives notifications when a new command is staged. |

## Usage Guide
1. **Staging**: When a player without bypass permission types a critical command (e.g., `/op TestUser`), the command is blocked.
2. **Notification**: Admins are notified that a command is pending review.
3. **Reviewing**: An authorized staff member types `/review`.
4. **Decision**:
   - Click **Green Wool** to **APPROVE** and execute the command.
   - Click **Red Wool** to **REJECT** and discard the command.

## Support
For support, bug reports, or feature requests, please join our Discord community:
**[Discord Server](https://discord.gg/bdxGxCbqCj)**

---
*Developed by DevPlugins*
