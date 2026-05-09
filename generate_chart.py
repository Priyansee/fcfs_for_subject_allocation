import matplotlib.pyplot as plt
import numpy as np

# Real Benchmark Data
concurrency_levels = ['100', '200', '500', '1000']
successful_allocations = [20, 20, 20, 20]
rejected_allocations = [80, 180, 480, 980]

x = np.arange(len(concurrency_levels))
width = 0.35

fig, ax = plt.subplots(figsize=(8, 5))
rects1 = ax.bar(x - width/2, successful_allocations, width, label='Successful', color='#4CAF50')
rects2 = ax.bar(x + width/2, rejected_allocations, width, label='Rejected', color='#F44336')

ax.set_xlabel('Concurrency Level (Number of Threads)')
ax.set_ylabel('Number of Allocations')
ax.set_title('Successful vs Rejected Allocations Across Concurrency Levels')
ax.set_xticks(x)
ax.set_xticklabels(concurrency_levels)
ax.legend()

ax.bar_label(rects1, padding=3)
ax.bar_label(rects2, padding=3)

fig.tight_layout()
plt.savefig('Fig_4_allocations_chart_real.png', dpi=300)
print("Saved as Fig_4_allocations_chart_real.png")
