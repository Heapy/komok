function handleCreated(id, bookmarkInfo) {
    console.log(`New bookmark ID: ${id}`);
    console.log(`New bookmark URL: ${bookmarkInfo.url}`);
}

browser.bookmarks.onCreated.addListener(handleCreated);

function handleRemoved(id, removeInfo) {
    console.log(`Item: ${id} removed`);
    console.log(`Title: ${removeInfo.node.title}`);
    console.log(`Url: ${removeInfo.node.url}`);
}

browser.bookmarks.onRemoved.addListener(handleRemoved);
