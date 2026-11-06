# Endgame gear on mobs (for pack makers)

Everything beyond Omnipotence runs on **Eschaton Charge**, kept on the item in the data component
`theoreticalsciences:eschaton_charge` (a whole number). An item with none is *dormant*. A mob given an endgame item with
no charge (a plain `/summon ... HandItems` or a KubeJS spawn without the component) gets a dormant one.

## What each piece does on a mob

| Item | Charged, on a mob | Dormant, on a mob |
|---|---|---|
| Omnipotent Edge (main hand) | Hits with its full weight (+199 attack damage on top of the mob's own). Each blow spends 20 charge (20,000 full: 1,000 blows), then it is only a blade. Mobs never get the Edge's unmaking (that is a player's alone), so it never goes through armour or invulnerability when a mob swings it. | +3 attack damage. |
| Dodecaract Excavator (main hand) | A pickaxe as a weapon (+8 attack damage). It mines nothing for a mob. | The same. |
| Scepter, Codex, Censer, Hand, Anchor | Held only: mobs never use items, so none of their powers act. | The same. |
| Crown of Transcendence | Armour only (its night vision and water breathing are a player's). | No armour. |
| Mantle of Omnipotence | Armour; the mob takes half damage and no fire damage (it does not burn in sunlight). | No armour. |
| Greaves of the Absolute | Armour; +40% movement speed, so it walks and chases faster; steps up a block. (The push forward is a player's.) | No armour. |
| Treads of Infinity | Armour; no fall damage, higher jumps. | No armour. |
| The whole Raiment | The mob takes no damage at all except from what ignores invulnerability (`/kill`, the void, the Omnipotent Edge). | Nothing. |

**Every charged piece runs down on a mob** as it does on a player: the Raiment spends 5 charge every 5 seconds while worn
(20,000 lasts about 5.5 hours of being loaded) and 10 for each point of damage it keeps away (at most 2,000 a blow). So a
mob in the whole charged Raiment is untouchable only until enough blows have drained it, or at once to the Edge.

## AI

Mobs keep their own AI: nothing in the mod changes a mob's goals, targeting or pathfinding for what it holds or wears.
Zombie-like mobs that pick up loot prefer the Edge over other swords (it is a sword with more damage) and will pick up
endgame gear players drop, charge and all. A mob's attack speed is its own (the item's attack speed is ignored by mob
AI). Nothing in this gear casts a mob to a player, so none of it can crash on a mob.

## Giving a mob charged gear

KubeJS (server scripts), for example:

```js
EntityEvents.spawned('minecraft:zombie', event => {
  const z = event.entity
  if (Math.random() < 0.01) {
    z.setItemSlot('mainhand', Item.of('theoreticalsciences:omnipotent_edge', '[theoreticalsciences:eschaton_charge=2000]'))
    z.setDropChance('mainhand', 0.0)
  }
})
```

Or a command: `/summon minecraft:zombie ~ ~ ~ {HandItems:[{id:"theoreticalsciences:omnipotent_edge",count:1,components:{"theoreticalsciences:eschaton_charge":2000}},{}]}`

Set the drop chances to taste: by default a mob drops its equipment rarely, and a dropped charged item keeps its charge.
