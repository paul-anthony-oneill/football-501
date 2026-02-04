<script lang="ts">
    import { page } from '$app/stores';

    export let open = true;

    const links = [
        { href: '/admin/categories', label: 'Categories', icon: '📁' },
        { href: '/admin/questions', label: 'Questions', icon: '❓' },
    ];

    $: currentPath = $page.url.pathname;
</script>

<aside class:open>
    <div class="logo">
        <h2>Football 501</h2>
        <span class="badge">Admin</span>
    </div>

    <nav>
        <ul>
            {#each links as link}
                <li>
                    <a
                        href={link.href}
                        class:active={currentPath.startsWith(link.href)}
                    >
                        <span class="icon">{link.icon}</span>
                        <span class="label">{link.label}</span>
                    </a>
                </li>
            {/each}
        </ul>
    </nav>

    <div class="footer">
        <a href="/" class="back-link">
            <span class="icon">⬅️</span>
            <span class="label">Back to Game</span>
        </a>
    </div>
</aside>

<style>
    aside {
        width: 250px;
        height: 100vh;
        background: #1a1a1a;
        border-right: 1px solid #333;
        display: flex;
        flex-direction: column;
        transition: transform 0.3s ease;
        position: fixed;
        left: 0;
        top: 0;
        z-index: 100;
    }

    .logo {
        padding: 1.5rem;
        border-bottom: 1px solid #333;
        display: flex;
        align-items: center;
        gap: 0.5rem;
    }

    h2 {
        margin: 0;
        font-size: 1.25rem;
        color: #4ade80;
    }

    .badge {
        background: #333;
        padding: 0.25rem 0.5rem;
        border-radius: 4px;
        font-size: 0.75rem;
        color: #9ca3af;
        text-transform: uppercase;
    }

    nav {
        flex: 1;
        padding: 1rem;
    }

    ul {
        list-style: none;
        padding: 0;
        margin: 0;
    }

    li {
        margin-bottom: 0.5rem;
    }

    a {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        padding: 0.75rem 1rem;
        color: #9ca3af;
        text-decoration: none;
        border-radius: 6px;
        transition: all 0.2s;
    }

    a:hover {
        background: #2a2a2a;
        color: #fff;
    }

    a.active {
        background: rgba(74, 222, 128, 0.1);
        color: #4ade80;
        border-left: 3px solid #4ade80;
    }

    .icon {
        font-size: 1.25rem;
    }

    .footer {
        padding: 1rem;
        border-top: 1px solid #333;
    }

    .back-link {
        color: #ef4444;
    }

    .back-link:hover {
        background: rgba(239, 68, 68, 0.1);
        color: #ef4444;
    }
</style>
