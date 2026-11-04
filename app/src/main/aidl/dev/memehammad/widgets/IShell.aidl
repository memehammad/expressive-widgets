package dev.memehammad.widgets;

// Shizuku user service running as the shell user.
interface IShell {
    String exec(String cmd) = 1;

    // Reserved transaction code Shizuku uses to stop a user service.
    void destroy() = 16777114;
}
