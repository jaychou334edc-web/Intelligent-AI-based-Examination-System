# AI Examination System (AES)

# UI Design System Specification

Version: 2.0

Status: Engineering UI Standard

Audience:

- Codex
- Web UI Developers
- Frontend Engineers
- System Designers

# 1. UI Design Philosophy

The UI system is designed based on 4 principles.

## 1.1 Clarity First

Every UI must answer:

What can I do here?

No decorative-only components.

## 1.2 Role Separation

Teacher UI, Student UI, and Admin UI are separate workspaces.

They share only base components and design tokens.

They must not share the same information architecture.

## 1.3 Information Density Balance

- High density for teachers
- Medium density for admin
- Low distraction for students

## 1.4 Web-First Design

This is a browser-based web system.

UI must follow:

- Modern web application conventions
- Clear navigation
- Responsive layouts for common school monitors
- Keyboard-accessible interactions
- Browser examination constraints

# 2. UI Technology Direction

Frontend stack:

- Vue 3
- TypeScript
- Vite
- Element Plus
- Pinia
- Vue Router

The UI must not contain core business logic.

Business rules belong to the Java backend.

# 3. UI Style System

## 3.1 Visual Style Direction

We combine 3 proven UI styles:

### Admin Dashboard

- Data tables
- Filters
- Cards for metrics
- Clear action bars

### Professional Education System

- Calm colors
- Strong readability
- Predictable workflows

### Focused Exam Console

- Minimal distractions
- Large readable question area
- Clear timer
- Clear submission state

## 3.2 Color System

Primary Theme

- Primary Blue: #2563EB
- Success Green: #16A34A
- Warning Orange: #F59E0B
- Danger Red: #DC2626

Background

- Admin / Teacher: light professional dashboard by default
- Student Exam: high readability, low distraction

Base Colors

- Page Background: #F8FAFC
- Panel: #FFFFFF
- Border: #E2E8F0
- Text Primary: #0F172A
- Text Secondary: #64748B

## 3.3 Typography System

Font Priority:

1. system-ui
2. Microsoft YaHei
3. Segoe UI
4. Noto Sans

Rules:

- Page Title: 20-28px bold
- Section: 16-18px
- Body: 14px
- Hint: 12px

## 3.4 Layout Grid System

Base spacing unit:

8px system

Allowed spacing:

- 4px
- 8px
- 16px
- 24px
- 32px

Admin and teacher layout:

```text
Top Bar
Sidebar 240px
Main Workspace
Optional Right Drawer
```

Student exam layout:

```text
Top Timer Bar
Question Navigation
Question Workspace
Answer Area
Status Panel
```

# 4. Application Layout Architecture

## 4.1 Global Layout

After login, the frontend routes by role.

```text
+----------------------------------------+
| Top Bar                                |
+---------+------------------------------+
| Sidebar | Main Workspace               |
|         |                              |
+---------+------------------------------+
```

Student exam page may hide the normal sidebar to reduce distraction.

## 4.2 Sidebar Structure

Teacher sidebar contains:

- Dashboard
- Paper Management
- AI Parsing
- Question Bank
- Exam Management
- Grading
- Statistics
- Logs
- Settings

Admin sidebar contains:

- System Dashboard
- User Management
- Role Management
- System Logs
- Configuration
- Backup

Rules:

- Icons required
- Text label required
- Active highlight required
- No deeply nested menus

# 5. Teacher UI Design System

Teacher UI is the most complex system.

## 5.1 Teacher Dashboard

Components:

- Today's exams
- Pending grading tasks
- AI parsing status
- Recent activity logs
- Quick actions panel

Layout:

Metric cards plus task lists.

## 5.2 Paper Upload Page

UI Structure:

Left:

- Upload area
- File list

Right:

- Extracted text preview
- Parsing status

Bottom:

- Start AI Parsing button

## 5.3 AI Parsing Review Page

This is the most important UI in the system.

Layout:

```text
Left Panel:
- Parsed question list

Center Panel:
- Question editor

Right Panel:
- AI raw output
- Confidence score
- Validation warnings
```

Features:

- Inline editing
- Question split/merge
- Option editing
- Score adjustment
- AI confidence indicator
- Validation error display

## 5.4 Question Bank UI

Table-based layout:

Columns:

- Question ID
- Type
- Difficulty
- Knowledge Point
- Score
- Status

Features:

- Search bar
- Filter panel
- Batch actions
- Preview drawer

## 5.5 Exam Creation UI

Wizard-based UI:

Step 1: Basic Info

Step 2: Select Questions

Step 3: Configure Rules

Step 4: Publish

## 5.6 Grading UI

Layout:

Left:

- Student list

Center:

- Answer sheet

Right:

- AI suggested score
- Teacher score input
- Comment box

## 5.7 Statistics Dashboard

Charts:

- Score distribution
- Pass rate
- Question accuracy
- Difficulty heatmap
- Knowledge point analysis

Use card plus chart hybrid layout.

# 6. Student UI Design System

## 6.1 Student Dashboard

Minimal design:

- Active exams
- Past results
- Notifications

## 6.2 Exam Interface

Layout:

```text
Top:
- Timer
- Network status
- Submit button

Left:
- Question navigation

Center:
- Question content
- Answer controls

Right:
- Answered / unanswered status
- Anti-cheat warning count
```

Rules:

- Request browser fullscreen mode
- Auto-save every 10 seconds
- No distractions
- Keyboard navigation supported
- Clear warning when focus is lost
- Clear warning before final submission

## 6.3 Answer Types UI

Single Choice

Radio buttons

Multiple Choice

Checkbox group

Fill Blank

Inline input fields

Subjective

Large text area with autosave indicator

# 7. Admin UI Design

## 7.1 System Dashboard

- User count
- Exam count
- System logs
- AI usage stats
- Storage usage

## 7.2 User Management UI

Table plus drawer editing

- Add user
- Edit user
- Disable user
- Import students
- Export users

# 8. Component Design System

Base components should be reusable Vue components:

- AppButton
- AppCard
- AppTable
- AppDialog
- AppDrawer
- AppSidebar
- AppTopbar
- AppToast
- AppFormItem
- AppStatusTag
- AppEmptyState

# 9. Interaction Rules

- All actions must have loading state.
- Destructive actions require confirmation.
- Forms must validate before submit.
- Success and failure must show clear feedback.
- Long running AI tasks must show progress or polling status.
- No hidden critical actions.

# 10. State Design System

Every UI must support states:

- Loading
- Empty
- Error
- Success
- Disabled

No UI component is allowed without state handling.

# 11. Accessibility Rules

- Keyboard navigation required.
- Tab order must be logical.
- Contrast ratio must be readable.
- Font scaling supported.
- Important buttons must have text labels.

# 12. UI Performance Requirements

- Page switch < 200ms after resources loaded
- Table render < 500ms for normal data
- Large exam load < 1s in campus LAN
- No UI freeze during AI calls
- Exam autosave must not block typing

# 13. Anti-Pattern Rules

Forbidden:

- Overly complex dashboards
- Too many colors
- Hidden actions
- Deep nested menus
- Unlabeled icons
- Student exam page with marketing-style clutter
- Core grading rules in frontend code

# 14. Design Inspiration Sources

This system is influenced by:

- Element Plus admin patterns
- Ant Design Pro information architecture
- Microsoft Fluent UI principles
- Modern educational assessment platforms

But it must not directly copy any specific product UI.

# 15. Final UI Goal

The system UI should feel like:

- A professional web control center for administrators
- A powerful exam management workspace for teachers
- A clean browser exam console for students

Not a public marketing website.

Not a simple form system.

Not a generic student portal.

# 16. Definition of UI Success

UI is considered successful when:

- Teacher can manage exams without confusion.
- AI parsing results are visually editable.
- Student can complete exams without distraction.
- No core action requires explanation to use.
- All actions are discoverable.
