function toggleCategoryNav(name) {
        var sub = document.getElementById('subnav-' + name);
        var arrow = document.getElementById('arrow-' + name);
        if (!sub) return;
        var isHidden = (sub.style.display === 'none' || getComputedStyle(sub).display === 'none');
        if (isHidden) {
            sub.style.display = 'flex';
            if (arrow) arrow.classList.add('expanded');
        } else {
            sub.style.display = 'none';
            if (arrow) arrow.classList.remove('expanded');
        }
    }
